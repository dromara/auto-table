package org.dromara.autotable.core.strategy.mysql;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.mysql.builder.CreateTableSqlBuilder;
import org.dromara.autotable.core.strategy.mysql.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.core.strategy.mysql.builder.MysqlTableMetadataBuilder;
import org.dromara.autotable.core.strategy.mysql.data.MySqlDefaultTypeEnum;
import org.dromara.autotable.core.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlCompareTableInfo;
import org.dromara.autotable.core.strategy.mysql.data.MysqlTableMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlTypeHelper;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaColumn;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaStatistics;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaTable;
import org.dromara.autotable.core.strategy.mysql.mapper.MysqlTablesMapper;
import org.dromara.autotable.core.utils.StringUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目启动时自动扫描配置的目录中的model，根据配置的规则自动创建或更新表 该逻辑只适用于mysql，其他数据库尚且需要另外扩展，因为sql的语法不同
 *
 * @author sunchenbin, Spet
 * @version 2019/07/06
 */
@Slf4j
public class MysqlStrategy implements IStrategy<MysqlTableMetadata, MysqlCompareTableInfo, MysqlTablesMapper> {

    @Override
    public String databaseDialect() {
        return DatabaseDialect.MySQL;
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            put(String.class, MySqlDefaultTypeEnum.VARCHAR);
            put(Character.class, MySqlDefaultTypeEnum.CHAR);
            put(char.class, MySqlDefaultTypeEnum.CHAR);

            put(BigInteger.class, MySqlDefaultTypeEnum.BIGINT);
            put(Long.class, MySqlDefaultTypeEnum.BIGINT);
            put(long.class, MySqlDefaultTypeEnum.BIGINT);

            put(Integer.class, MySqlDefaultTypeEnum.INT);
            put(int.class, MySqlDefaultTypeEnum.INT);

            put(Boolean.class, MySqlDefaultTypeEnum.BIT);
            put(boolean.class, MySqlDefaultTypeEnum.BIT);

            put(Float.class, MySqlDefaultTypeEnum.FLOAT);
            put(float.class, MySqlDefaultTypeEnum.FLOAT);
            put(Double.class, MySqlDefaultTypeEnum.DOUBLE);
            put(double.class, MySqlDefaultTypeEnum.DOUBLE);
            put(BigDecimal.class, MySqlDefaultTypeEnum.DECIMAL);

            put(Date.class, MySqlDefaultTypeEnum.DATETIME);
            put(java.sql.Date.class, MySqlDefaultTypeEnum.DATE);
            put(java.sql.Timestamp.class, MySqlDefaultTypeEnum.DATETIME);
            put(java.sql.Time.class, MySqlDefaultTypeEnum.TIME);
            put(LocalDateTime.class, MySqlDefaultTypeEnum.DATETIME);
            put(LocalDate.class, MySqlDefaultTypeEnum.DATE);
            put(LocalTime.class, MySqlDefaultTypeEnum.TIME);

            put(Short.class, MySqlDefaultTypeEnum.SMALLINT);
            put(short.class, MySqlDefaultTypeEnum.SMALLINT);
        }};
    }

    @Override
    public String dropTable(String schema, String tableName) {

        return String.format("DROP TABLE IF EXISTS `%s`", tableName);
    }

    @Override
    public @NonNull MysqlTableMetadata analyseClass(Class<?> beanClass) {

        return MysqlTableMetadataBuilder.build(beanClass);
    }

    @Override
    public List<String> createTable(MysqlTableMetadata tableMetadata) {
        String sql = CreateTableSqlBuilder.buildSql(tableMetadata);
        return Collections.singletonList(sql);
    }

    @Override
    public @NonNull MysqlCompareTableInfo compareTable(MysqlTableMetadata tableMetadata) {

        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();
        MysqlCompareTableInfo mysqlCompareTableInfo = new MysqlCompareTableInfo(tableName, schema);

        InformationSchemaTable informationSchemaTable = executeReturn(mysqlTablesMapper -> mysqlTablesMapper.findTableByTableName(tableName));

        // 对比表配置有无变化
        compareTableProperties(tableMetadata, informationSchemaTable, mysqlCompareTableInfo);

        // 开始比对列的变化: 新增、修改、删除
        compareColumns(tableMetadata, tableName, mysqlCompareTableInfo);

        // 开始比对 主键 和 索引 的变化
        List<InformationSchemaStatistics> informationSchemaStatistics = executeReturn(mysqlTablesMapper -> mysqlTablesMapper.queryTablePrimaryAndIndex(tableName));
        // 按照主键（固定值：PRIMARY）、索引名字，对所有列进行分组
        Map<String, List<InformationSchemaStatistics>> keyColumnGroupByName = informationSchemaStatistics.stream()
                .collect(Collectors.groupingBy(InformationSchemaStatistics::getIndexName));

        // 对比主键
        List<InformationSchemaStatistics> tablePrimaries = keyColumnGroupByName.remove("PRIMARY");
        comparePrimary(tableMetadata, mysqlCompareTableInfo, tablePrimaries);

        // 对比索引, informationSchemaKeyColumnUsages中剩余的都是索引数据了
        Map<String, List<InformationSchemaStatistics>> tableIndexes = keyColumnGroupByName;
        compareIndexes(tableMetadata, mysqlCompareTableInfo, tableIndexes);

        return mysqlCompareTableInfo;
    }

    @Override
    public List<String> modifyTable(MysqlCompareTableInfo mysqlCompareTableInfo) {
        String sql = ModifyTableSqlBuilder.buildSql(mysqlCompareTableInfo);
        return Collections.singletonList(sql);
    }

    private void compareIndexes(MysqlTableMetadata mysqlTableMetadata, MysqlCompareTableInfo mysqlCompareTableInfo, Map<String, List<InformationSchemaStatistics>> tableIndexes) {
        // Bean上所有的索引
        List<IndexMetadata> indexMetadataList = mysqlTableMetadata.getIndexMetadataList();
        // 以Bean上的索引开启循环，逐个匹配表上的索引
        for (IndexMetadata indexMetadata : indexMetadataList) {
            // 根据Bean上的索引名称获取表上的索引
            String indexName = indexMetadata.getName();
            // 获取表上对应索引名称的所有列
            List<InformationSchemaStatistics> theIndexColumns = tableIndexes.remove(indexName);
            if (theIndexColumns == null) {
                // 表上不存在该索引，新增
                mysqlCompareTableInfo.getIndexMetadataList().add(indexMetadata);
            } else {
                // 先把表上的该索引的所有字段，按照顺序排列
                theIndexColumns = theIndexColumns.stream()
                        .sorted(Comparator.comparing(InformationSchemaStatistics::getSeqInIndex))
                        .collect(Collectors.toList());
                // 获取Bean上该索引涉及的所有字段（按照字段顺序自然排序）
                List<IndexMetadata.IndexColumnParam> columns = indexMetadata.getColumns();
                // 先初步按照索引牵扯的字段数量一不一样判断是不是需要更新索引
                if (theIndexColumns.size() != columns.size()) {
                    // 同名的索引，但是表上的字段数量跟Bean上指定的不一致，需要修改（先删除，再新增）
                    mysqlCompareTableInfo.getDropIndexList().add(indexName);
                    mysqlCompareTableInfo.getIndexMetadataList().add(indexMetadata);
                } else {
                    // 牵扯的字段数目一致，再按顺序逐个比较每个位置的列名及其排序方式是否相同
                    for (int i = 0; i < theIndexColumns.size(); i++) {
                        InformationSchemaStatistics informationSchemaStatistics = theIndexColumns.get(i);
                        IndexSortTypeEnum indexSort = IndexSortTypeEnum.parseFromMysql(informationSchemaStatistics.getCollation());
                        IndexMetadata.IndexColumnParam indexColumnParam = columns.get(i);
                        IndexSortTypeEnum indexColumnParamSort = indexColumnParam.getSort();

                        // 名字不同即不同
                        boolean nameIsDiff = !informationSchemaStatistics.getColumnName().equals(indexColumnParam.getColumn());
                        // 类注解指定排序方式不为空的情况下，与库中的值不同即不同
                        boolean sortTypeIsDiff = indexColumnParamSort != null && indexColumnParamSort != indexSort;
                        if (nameIsDiff || sortTypeIsDiff) {
                            // 同名的索引，但是表上的字段数量跟Bean上指定的不一致，需要修改（先删除，再新增）
                            mysqlCompareTableInfo.getDropIndexList().add(indexName);
                            mysqlCompareTableInfo.getIndexMetadataList().add(indexMetadata);
                            break;
                        }
                    }
                }
            }
        }
        // 因为上一步循环，在基于Bean上索引匹配上表中的索引后，就立即删除了表上对应的索引，所以剩下的索引都是Bean上没有声明的索引，需要根据配置判断，是否删掉多余的索引
        Set<String> needDropIndexes = tableIndexes.keySet();
        if (!needDropIndexes.isEmpty()) {
            PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();
            // 删除autotable创建的索引
            if (autoTableProperties.getAutoDropIndex()) {
                List<String> autoTableCreateIndexes = needDropIndexes.stream().filter(indexName -> indexName.startsWith(autoTableProperties.getIndexPrefix())).collect(Collectors.toList());
                mysqlCompareTableInfo.getDropIndexList().addAll(autoTableCreateIndexes);
            }
            // 删除手动创建的索引
            if (autoTableProperties.getAutoDropCustomIndex()) {
                List<String> customCreateIndexes = needDropIndexes.stream().filter(indexName -> !indexName.startsWith(autoTableProperties.getIndexPrefix())).collect(Collectors.toList());
                mysqlCompareTableInfo.getDropIndexList().addAll(customCreateIndexes);
            }
        }
    }

    private static void comparePrimary(MysqlTableMetadata mysqlTableMetadata, MysqlCompareTableInfo mysqlCompareTableInfo, List<InformationSchemaStatistics> tablePrimaries) {

        // 获取当前Bean上指定的主键列表，顺序按照列的自然顺序排列
        List<MysqlColumnMetadata> primaries = mysqlTableMetadata.getColumnMetadataList().stream()
                .filter(MysqlColumnMetadata::isPrimary)
                .collect(Collectors.toList());

        // 库里不存在主键，实体上指定了
        boolean tableNoPrimary = tablePrimaries == null || tablePrimaries.isEmpty();
        boolean entityHasPrimary = !primaries.isEmpty();
        if (tableNoPrimary && entityHasPrimary) {
            // 添加新主键
            mysqlCompareTableInfo.setNewPrimaries(primaries);
        }
        // 库里存在主键，实体上不存在主键
        if (!tableNoPrimary && !entityHasPrimary) {
            // 删除已有主键
            mysqlCompareTableInfo.setDropPrimary(true);
        }
        // 库里存在主键，且实体上指定了主键，开始比对
        if (!tableNoPrimary && entityHasPrimary) {

            boolean sameSize = tablePrimaries.size() == primaries.size();
            // 主键数量不一致，主键全盘更新
            boolean needResetPrimary = !sameSize;
            // 主键数量一致的情况下，逐个比对每个位置的列名
            if (sameSize) {
                // 先按照顺序排好数据库主键的顺序
                tablePrimaries = tablePrimaries.stream()
                        .sorted(Comparator.comparing(InformationSchemaStatistics::getSeqInIndex))
                        .collect(Collectors.toList());
                for (int i = 0; i < tablePrimaries.size(); i++) {
                    // 获取Bean对应的位置的主键比对
                    InformationSchemaStatistics tablePrimary = tablePrimaries.get(i);
                    if (!tablePrimary.getColumnName().equals(primaries.get(i).getName())) {
                        // 主键列中按顺序比较，存在顺序不一致的情况，需要更新
                        needResetPrimary = true;
                        break;
                    }
                }
            }

            if (needResetPrimary) {
                mysqlCompareTableInfo.resetPrimary(primaries);
            }
        }
    }

    /**
     * 对比列的不同
     */
    private void compareColumns(MysqlTableMetadata mysqlTableMetadata, String tableName, MysqlCompareTableInfo mysqlCompareTableInfo) {
        // 实体全部字段描述
        List<MysqlColumnMetadata> mysqlColumnMetadataList = mysqlTableMetadata.getColumnMetadataList();
        // 变形：《列名，实体字段描述》
        Map<String, MysqlColumnMetadata> columnParamMap = mysqlColumnMetadataList.stream().collect(Collectors.toMap(MysqlColumnMetadata::getName, Function.identity()));
        // 查询数据库所有列数据
        List<InformationSchemaColumn> tableColumnList = executeReturn(mysqlTablesMapper -> mysqlTablesMapper.findTableEnsembleByTableName(tableName));

        // 获取顺序变更的sql
        ColumnPositionHelper.generateChangePosition(tableColumnList, mysqlColumnMetadataList);

        for (InformationSchemaColumn informationSchemaColumn : tableColumnList) {
            String columnName = informationSchemaColumn.getColumnName();
            // 以数据库字段名，从当前Bean上取信息，获取到就从中剔除
            MysqlColumnMetadata mysqlColumnMetadata = columnParamMap.remove(columnName);
            if (mysqlColumnMetadata != null) {
                // 取到了，则进行字段配置的比对
                boolean columnPositionChanged = mysqlColumnMetadata.getNewPreColumn() != null;
                boolean commentChanged = isCommentChanged(informationSchemaColumn, mysqlColumnMetadata);
                boolean fieldTypeChanged = isFieldTypeChanged(informationSchemaColumn, mysqlColumnMetadata);
                boolean notNullChanged = mysqlColumnMetadata.isNotNull() != informationSchemaColumn.isNotNull();
                boolean fieldIsAutoIncrementChanged = mysqlColumnMetadata.isAutoIncrement() != informationSchemaColumn.isAutoIncrement();
                boolean defaultValueChanged = isDefaultValueChanged(informationSchemaColumn, mysqlColumnMetadata);
                boolean charsetChanged = isCharsetChanged(informationSchemaColumn, mysqlColumnMetadata);
                if (columnPositionChanged || commentChanged || fieldTypeChanged || notNullChanged || fieldIsAutoIncrementChanged || defaultValueChanged || charsetChanged) {
                    // 任何一项有变化，则说明需要更新该字段
                    mysqlCompareTableInfo.addEditColumnMetadata(mysqlColumnMetadata);
                }
            } else {
                // 没有取到对应字段，说明库中存在的字段，Bean上不存在，根据配置，决定是否删除库上的多余字段
                if (AutoTableGlobalConfig.getAutoTableProperties().getAutoDropColumn()) {
                    mysqlCompareTableInfo.getDropColumnList().add(columnName);
                }
            }
        }
        // 因为按照表中字段已经晒过一轮Bean上的字段了，同名可以取到的均删除了，剩下的都是表中字段不存在的，需要新增
        Collection<MysqlColumnMetadata> needNewColumns = columnParamMap.values();
        for (MysqlColumnMetadata needNewColumn : needNewColumns) {
            mysqlCompareTableInfo.addNewColumnMetadata(needNewColumn);
        }
    }

    private static boolean isCharsetChanged(InformationSchemaColumn informationSchemaColumn, MysqlColumnMetadata mysqlColumnMetadata) {
        boolean charsetDiff = StringUtils.hasText(mysqlColumnMetadata.getCharacterSet()) && !Objects.equals(mysqlColumnMetadata.getCharacterSet(), informationSchemaColumn.getCharacterSetName());
        boolean collateDiff = StringUtils.hasText(mysqlColumnMetadata.getCollate()) && !Objects.equals(mysqlColumnMetadata.getCollate(), informationSchemaColumn.getCollationName());
        return charsetDiff || collateDiff;
    }

    private static boolean isDefaultValueChanged(InformationSchemaColumn informationSchemaColumn, MysqlColumnMetadata mysqlColumnMetadata) {
        String columnDefault = informationSchemaColumn.getColumnDefault();
        DefaultValueEnum defaultValueType = mysqlColumnMetadata.getDefaultValueType();
        if (DefaultValueEnum.isValid(defaultValueType)) {
            // 需要设置为null，但是数据库当前不是null
            if (defaultValueType == DefaultValueEnum.NULL) {
                return columnDefault != null;
            }
            // 需要设置为空字符串，但是数据库当前不是空字符串
            if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
                return !"".equals(columnDefault);
            }
        } else {
            DatabaseTypeAndLength columnType = mysqlColumnMetadata.getType();
            // 如果是数据库是bit类型，默认值是b'1' 或者 b'0' 的形式
            if (MysqlTypeHelper.isBoolean(columnType) && columnDefault != null && columnDefault.startsWith("b'") && columnDefault.endsWith("'")) {
                columnDefault = columnDefault.substring(2, columnDefault.length() - 1);
            }
            // 自定义值 默认值对比
            String defaultValue = mysqlColumnMetadata.getDefaultValue();
            // mysql中，如果是数据库浮点数，默认值后面会带上对应的小数位数，策略：数据库值于注解指定的值，均去掉多余的0进行对比
            if (MysqlTypeHelper.isFloatNumber(columnType) && columnDefault != null && columnDefault.matches(StringUtils.NUMBER_REGEX)) {
                // 先转数字，再转字符串，消除多余的0，异常忽略
                try {
                    columnDefault = String.valueOf(Double.parseDouble(columnDefault));
                } catch (Exception ignore) {
                }
                try {
                    defaultValue = String.valueOf(Double.parseDouble(defaultValue));
                } catch (Exception ignore) {
                }
            }
            // 兼容逻辑：如果是需要字符串兼容的类型（字符串、日期），使用者在默认值前后携带了单引号（'）的话，则在比对的时候自动去掉
            if (MysqlTypeHelper.needStringCompatibility(columnType) && defaultValue != null && defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
                defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
            }
            return !Objects.equals(defaultValue, columnDefault);
        }
        return false;
    }

    /**
     * 字段类型比对是否需要改变
     */
    private static boolean isFieldTypeChanged(InformationSchemaColumn informationSchemaColumn, MysqlColumnMetadata mysqlColumnMetadata) {

        DatabaseTypeAndLength fieldType = mysqlColumnMetadata.getType();

        // 非整数类型，类型全文匹配：varchar(255) double(6,2) enum('A','B')
        String fullType = MysqlTypeHelper.getFullType(fieldType);

        // bigint(20) unsigned zerofill
        String dbColumnTypeStr = informationSchemaColumn.getColumnType();
        List<String> dbColumnTypeArr = Arrays.asList(dbColumnTypeStr.split(" "));
        String dbColumnType = dbColumnTypeArr.get(0);

        /* 判断其他类型是否相同 */
        boolean isTypeDiff;
        // 枚举类的，不忽略大小写比较
        if (MysqlTypeHelper.isEnum(fieldType)) {
            isTypeDiff = !fullType.equals(dbColumnType);
        }
        // 整数类型，先对比完整的类型是否相同（比如：int(10) == int(10)，或者int == int(10)），据观察
        // 正常情况下int后面是没有长度的，但是使用了zerofill后，需要长度，因此，存在两种情况：
        // 1、注解：int(10)，数据库：int(10)，该情况是注解显示的指定了类型长度
        // 2、注解：int，数据库：int(10)，该情况是注解没有指定类型长度，数据库可能是默认的长度（指定了zerofill会默认配置长度），也可能是手动指定的
        else if (MysqlTypeHelper.isNoLengthNumber(fieldType)) {
            // dbColumnType是全类型，informationSchemaColumn.getDataType()只有数据类型
            // 所以，当注解指定的类型，既不是int(10)这种的，也不是int这种的，说明类型的确不一样
            isTypeDiff = !fullType.equalsIgnoreCase(dbColumnType) && !fullType.equalsIgnoreCase(informationSchemaColumn.getDataType());
        } else {
            // 剩下的忽略大小写进行比较
            isTypeDiff = !fullType.equalsIgnoreCase(dbColumnType);
        }


        /* 判断限定符是否相同 */
        boolean dbHasQualifier = dbColumnTypeArr.size() > 1;
        // 设置初始值
        boolean isQualifierDiff = false;
        // 任何一方有限定符的话，则进行比较
        if (mysqlColumnMetadata.hasQualifier() || dbHasQualifier) {
            // 无符号比较
            if (mysqlColumnMetadata.isUnsigned() != dbColumnTypeArr.stream().anyMatch("unsigned"::equalsIgnoreCase)) {
                isQualifierDiff = true;
            }
            // 零填充比较
            else if (mysqlColumnMetadata.isZerofill() != dbColumnTypeArr.stream().anyMatch("zerofill"::equalsIgnoreCase)) {
                isQualifierDiff = true;
            }
        }

        return isTypeDiff || isQualifierDiff;
    }

    private static boolean isCommentChanged(InformationSchemaColumn informationSchemaColumn, MysqlColumnMetadata mysqlColumnMetadata) {
        String fieldComment = mysqlColumnMetadata.getComment();
        String dbColumnComment = informationSchemaColumn.getColumnComment();
        return (StringUtils.hasText(fieldComment) || StringUtils.hasText(dbColumnComment)) && !fieldComment.equals(dbColumnComment);
    }

    private static void compareTableProperties(MysqlTableMetadata mysqlTableMetadata, InformationSchemaTable tableInformation, MysqlCompareTableInfo mysqlCompareTableInfo) {
        String tableComment = mysqlTableMetadata.getComment();
        String tableCharset = mysqlTableMetadata.getCharacterSet();
        String tableCollate = mysqlTableMetadata.getCollate();
        String tableEngine = mysqlTableMetadata.getEngine();
        // 判断表注释是否要更新
        if (StringUtils.hasText(tableComment) && !tableComment.equals(tableInformation.getTableComment())) {
            mysqlCompareTableInfo.setComment(tableComment);
        }
        // 判断表字符集是否要更新
        if (StringUtils.hasText(tableCharset)) {
            String collate = tableInformation.getTableCollation();
            if (StringUtils.hasText(collate)) {
                String charset = collate.substring(0, collate.indexOf("_"));
                if (!tableCharset.equals(charset) || !tableCollate.equals(collate)) {
                    mysqlCompareTableInfo.setCharacterSet(tableCharset);
                    mysqlCompareTableInfo.setCollate(tableCollate);
                }
            }
        }
        // 判断表引擎是否要更新
        if (StringUtils.hasText(tableEngine) && !tableEngine.equals(tableInformation.getEngine())) {
            mysqlCompareTableInfo.setEngine(tableEngine);
        }
    }
}
