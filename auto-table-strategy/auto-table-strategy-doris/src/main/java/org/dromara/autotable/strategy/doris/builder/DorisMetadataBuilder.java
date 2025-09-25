package org.dromara.autotable.strategy.doris.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.doris.DorisColumn;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.annotation.doris.emuns.AggregateFun;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.strategy.doris.DorisHelper;
import org.dromara.autotable.strategy.doris.DorisStrategy;
import org.dromara.autotable.core.utils.BeanClassUtil;
import org.dromara.autotable.core.utils.TableMetadataHandler;
import org.dromara.autotable.strategy.doris.data.DorisColumnMetadata;
import org.dromara.autotable.strategy.doris.data.DorisIndexMetadata;
import org.dromara.autotable.strategy.doris.data.DorisPartitionMetadata;
import org.dromara.autotable.strategy.doris.data.DorisRollupMetadata;
import org.dromara.autotable.strategy.doris.data.DorisTableMetadata;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author don
 */
@Slf4j
public class DorisMetadataBuilder {
    private static final ColumnMetadataBuilder defaultColumnMetadataBuilder = new ColumnMetadataBuilder(DorisStrategy.databaseDialect);


    /**
     * 根据实体类生成Doris表元数据
     *
     * @param clazz 实体类类对象
     * @return Doris表元数据对象
     * @throws IllegalStateException 如果实体类缺少必要的注解或配置，则抛出此异常
     */
    public static DorisTableMetadata buildTableMetadata(Class<?> clazz) {
        // 获取表名和表注释
        String tableName = TableMetadataHandler.getTableName(clazz);
        String tableComment = TableMetadataHandler.getTableComment(clazz);

        // 查找DorisTable注解
        DorisTable dorisTable = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder().find(clazz, DorisTable.class);
        if (dorisTable == null) {
            throw new IllegalStateException(clazz.getSimpleName() + "缺少@" + DorisTable.class.getSimpleName());
        }
        // 获取字段和列信息
        List<Field> fields = BeanClassUtil.sortAllFieldForColumn(clazz);
        AtomicInteger fieldIndex = new AtomicInteger(1);
        List<DorisColumnMetadata> columnMetadataList = fields.stream()
                .filter(field -> TableMetadataHandler.isIncludeField(field, clazz))
                .map(field -> {
                    int position = fieldIndex.getAndIncrement();
                    ColumnMetadata delegation = defaultColumnMetadataBuilder.build(clazz, field, position);
                    DorisColumnMetadata columnMetadata = new DorisColumnMetadata(delegation);
                    columnMetadata.setFieldName(field.getName());
                    columnMetadata.setPosition(position);
                    DorisColumn dorisColumn = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder().find(field, DorisColumn.class);
                    if (dorisColumn == null) {
                        return columnMetadata;
                    }
                    AggregateFun aggregateFun = dorisColumn.aggregateFun();
                    if (!aggregateFun.equals(AggregateFun.none)) {
                        columnMetadata.setAggregateFun(aggregateFun.name());
                    }
                    if (dorisColumn.autoIncrementStartValue() > 0) {
                        columnMetadata.setAutoIncrement(true);
                        columnMetadata.setAutoIncrementStartValue(dorisColumn.autoIncrementStartValue());
                    }
                    columnMetadata.setOnUpdateCurrentTimestamp(dorisColumn.onUpdateCurrentTimestamp());
                    return columnMetadata;
                })
                .sorted(Comparator.comparingInt(DorisColumnMetadata::getPosition))
                .collect(Collectors.toList());
        if (columnMetadataList.isEmpty()) {
            log.warn("扫描发现{}没有建表字段请注意！", clazz.getName());
        }
        Map<String, String> field2Column = DorisHelper.toMap(columnMetadataList, DorisColumnMetadata::getFieldName, DorisColumnMetadata::getName);

        // 处理索引信息
        List<DorisIndexMetadata> indexMetadataList = Arrays.stream(dorisTable.indexes())
                .map(dorisIndex -> {
                    String column = field2Column.getOrDefault(dorisIndex.column(), dorisIndex.column());
                    String type = dorisIndex.using().name();
                    String name = DorisHelper.getIndexName(dorisIndex.name(), column, type);
                    Map<String, String> properties = DorisHelper.parseProperties(dorisIndex.properties());
                    DorisIndexMetadata indexMetadata = new DorisIndexMetadata();
                    indexMetadata.setName(name);
                    indexMetadata.setColumn(column);
                    indexMetadata.setType(type);
                    indexMetadata.setProperties(properties);
                    indexMetadata.setComment(dorisIndex.comment());
                    return indexMetadata;
                })
                .collect(Collectors.toList());

        // 验证并处理键类型
        long keysCount = Stream.of(dorisTable.duplicate_key(), dorisTable.unique_key(), dorisTable.aggregate_key())
                .filter(it -> it.length > 0)
                .count();
        if (keysCount > 1) {
            throw new IllegalStateException("@" + DorisTable.class.getSimpleName() + "只能配置duplicate_key/unique_key/aggregate_key其中一个");
        }
        String keysType = null;
        List<String> keys = new ArrayList<>();
        if (dorisTable.duplicate_key().length > 0) {
            keysType = "duplicate key";
            keys = Arrays.stream(dorisTable.duplicate_key())
                    .map(key -> field2Column.getOrDefault(key, key))
                    .collect(Collectors.toList());
        }
        if (dorisTable.unique_key().length > 0) {
            keysType = "unique key";
            keys = Arrays.stream(dorisTable.unique_key())
                    .map(key -> field2Column.getOrDefault(key, key))
                    .collect(Collectors.toList());
        }
        if (dorisTable.aggregate_key().length > 0) {
            keysType = "aggregate key";
            keys = Arrays.stream(dorisTable.aggregate_key())
                    .map(key -> field2Column.getOrDefault(key, key))
                    .collect(Collectors.toList());
        }
        // 未配置duplicate_key/unique_key/aggregate_key情况下，使用主键作为unique key
        if (keys.isEmpty()) {
            keysType = "unique key";
            keys = columnMetadataList.stream()
                    .filter(ColumnMetadata::isPrimary)
                    .map(DorisColumnMetadata::getName)
                    .collect(Collectors.toList());
        }
        if (keys.isEmpty()) {
            throw new IllegalStateException("@" + DorisTable.class.getSimpleName() + "必须配置duplicate_key/unique_key/aggregate_key其中一个,或者配置至少一个主键");
        }

        // 标记key列
        for (DorisColumnMetadata columnMetadata : columnMetadataList) {
            columnMetadata.setKey(keys.contains(columnMetadata.getName()));
        }

        // 处理分区信息
        if (dorisTable.partition_by_range().length > 0 && dorisTable.partition_by_list().length > 0) {
            throw new IllegalStateException("@" + DorisTable.class.getSimpleName() + "只能配置partition_by_range/partition_by_list其中一个");
        }
        String partitionBy = "";
        List<String> partitionColumns = new ArrayList<>();

        if (dorisTable.partition_by_range().length > 0) {
            partitionBy = "range";
            partitionColumns = Arrays.stream(dorisTable.partition_by_range())
                    .map(it -> field2Column.getOrDefault(it, it))
                    .collect(Collectors.toList());
        }
        if (dorisTable.partition_by_list().length > 0) {
            partitionBy = "list";
            partitionColumns = Arrays.stream(dorisTable.partition_by_list())
                    .map(it -> field2Column.getOrDefault(it, it))
                    .collect(Collectors.toList());
        }
        int partitionColumnsSize = partitionColumns.size();
        AtomicInteger partitionIndex = new AtomicInteger(1);
        List<DorisPartitionMetadata.Partition> partitions = Arrays.stream(dorisTable.partitions())
                .map(dorisPartition -> {
                    int position = partitionIndex.getAndIncrement();
                    DorisPartitionMetadata.Partition partition = DorisPartitionMetadata.Partition.builder()
                            .position(position)
                            .columnSize(partitionColumnsSize)
                            .name(dorisPartition.partition())
                            .valuesLeftInclude(Arrays.asList(dorisPartition.values_left_include()))
                            .valuesRightExclude(Arrays.asList(dorisPartition.values_right_exclude()))
                            .valuesLessThan(Arrays.asList(dorisPartition.values_less_than()))
                            .from(dorisPartition.from())
                            .to(dorisPartition.to())
                            .interval(dorisPartition.interval())
                            .unit(dorisPartition.unit().getValue())
                            .valuesIn(Arrays.asList(dorisPartition.values_in()))
                            .build();
                    if (partition.getFrom().isEmpty() && partition.getName().isEmpty()) {
                        partition.setName("p" + position);
                    }
                    return partition;
                })
                .collect(Collectors.toList());
        Map<String, String> dynamicPartitionProperties = DorisHelper.getDynamicPartitionProperties(dorisTable.dynamic_partition());

        DorisPartitionMetadata partitionMetadata = DorisPartitionMetadata.builder()
                .autoPartition(dorisTable.auto_partition())
                .autoPartitionTimeUnit(dorisTable.auto_partition_time_unit().name())
                .partitionBy(partitionBy)
                .partitionColumns(partitionColumns)
                .partitions(partitions)
                .dynamicPartitionProperties(dynamicPartitionProperties)
                .build();

        // 处理分桶信息
        String distributedBy = dorisTable.distributed_by_hash().length > 0
                ? "hash" : "random";
        List<String> distributedColumns = Arrays.stream(dorisTable.distributed_by_hash())
                .map(it -> field2Column.getOrDefault(it, it))
                .collect(Collectors.toList());
        String distributedBuckets = dorisTable.distributed_buckets() > 0
                ? String.valueOf(dorisTable.distributed_buckets()) : "auto";

        // 处理Rollup信息
        List<DorisRollupMetadata> rollupMetadataList = Arrays.stream(dorisTable.rollup())
                .map(dorisRollup -> {
                    List<String> columns = Arrays.stream(dorisRollup.columns())
                            .map(it -> field2Column.getOrDefault(it, it))
                            .collect(Collectors.toList());
                    String rollupName = DorisHelper.getRollupName(dorisRollup.name(), columns);
                    return DorisRollupMetadata.builder()
                            .name(rollupName)
                            .columns(columns)
                            .properties(DorisHelper.parseProperties(dorisRollup.properties()))
                            .build();
                })
                .collect(Collectors.toList());

        // 合并属性
        Map<String, String> properties = DorisHelper.parseProperties(dorisTable.properties());
        properties.putAll(dynamicPartitionProperties);

        // 创建并返回Doris表元数据对象
        DorisTableMetadata tableMetadata = new DorisTableMetadata(clazz, tableName, tableComment);
        tableMetadata.setColumnMetadataList(columnMetadataList);
        tableMetadata.setIndexMetadataList(indexMetadataList);
        tableMetadata.setEngine(dorisTable.engine());
        tableMetadata.setKeysType(keysType);
        tableMetadata.setKeys(keys);
        tableMetadata.setPartitionMetadata(partitionMetadata);
        tableMetadata.setDistributedBy(distributedBy);
        tableMetadata.setDistributedColumns(distributedColumns);
        tableMetadata.setDistributedBuckets(distributedBuckets);
        tableMetadata.setRollupMetadataList(rollupMetadataList);
        tableMetadata.setProperties(properties);
        return tableMetadata;
    }


}
