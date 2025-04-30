package org.dromara.autotable.core.strategy.doris;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.doris.builder.DorisMetadataBuilder;
import org.dromara.autotable.core.strategy.doris.builder.DorisSqlBuilder;
import org.dromara.autotable.core.strategy.doris.data.DorisCompareTableInfo;
import org.dromara.autotable.core.strategy.doris.data.DorisDefaultTypeEnum;
import org.dromara.autotable.core.strategy.doris.data.DorisTableMetadata;
import org.dromara.autotable.core.strategy.doris.data.dbdata.InformationSchemaColumn;
import org.dromara.autotable.core.strategy.doris.mapper.DorisTablesMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 项目启动时自动扫描配置的目录中的model，根据配置的规则自动创建或更新表 该逻辑只适用于mysql，其他数据库尚且需要另外扩展，因为sql的语法不同
 *
 * @author sunchenbin, Spet
 * @version 2019/07/06
 */
@Slf4j
public class DorisStrategy implements IStrategy<DorisTableMetadata, DorisCompareTableInfo> {

    public static final String databaseDialect = DatabaseDialect.Doris;
    private final DorisTablesMapper mapper = new DorisTablesMapper();


    @Override
    public String databaseDialect() {
        return databaseDialect;
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            put(String.class, DorisDefaultTypeEnum.VARCHAR);
            put(Character.class, DorisDefaultTypeEnum.CHAR);
            put(char.class, DorisDefaultTypeEnum.CHAR);

            put(BigInteger.class, DorisDefaultTypeEnum.BIGINT);
            put(Long.class, DorisDefaultTypeEnum.BIGINT);
            put(long.class, DorisDefaultTypeEnum.BIGINT);

            put(Integer.class, DorisDefaultTypeEnum.INT);
            put(int.class, DorisDefaultTypeEnum.INT);

            put(Boolean.class, DorisDefaultTypeEnum.BOOLEAN);
            put(boolean.class, DorisDefaultTypeEnum.BOOLEAN);

            put(Float.class, DorisDefaultTypeEnum.FLOAT);
            put(float.class, DorisDefaultTypeEnum.FLOAT);
            put(Double.class, DorisDefaultTypeEnum.DOUBLE);
            put(double.class, DorisDefaultTypeEnum.DOUBLE);
            put(BigDecimal.class, DorisDefaultTypeEnum.DECIMAL);

            put(Date.class, DorisDefaultTypeEnum.DATETIME);
            put(java.sql.Date.class, DorisDefaultTypeEnum.DATE);
            put(java.sql.Timestamp.class, DorisDefaultTypeEnum.DATETIME);
            put(java.sql.Time.class, DorisDefaultTypeEnum.VARCHAR);
            put(LocalDateTime.class, DorisDefaultTypeEnum.DATETIME);
            put(LocalDate.class, DorisDefaultTypeEnum.DATE);
            put(LocalTime.class, DorisDefaultTypeEnum.VARCHAR);

            put(Short.class, DorisDefaultTypeEnum.SMALLINT);
            put(short.class, DorisDefaultTypeEnum.SMALLINT);
        }};
    }

    @Override
    public String dropTable(String schema, String tableName) {
        return String.format("drop table if exists `%s`", tableName);
    }

    @Override
    public @NonNull DorisTableMetadata analyseClass(Class<?> beanClass) {
        return DorisMetadataBuilder.buildTableMetadata(beanClass);
    }

    @Override
    public List<String> createTable(DorisTableMetadata tableMetadata) {
        String sql = DorisSqlBuilder.buildSql(tableMetadata);
        return Collections.singletonList(sql);
    }

    /**
     * 比较Doris表的结构信息
     * 该方法用于对比一个给定的Doris表和一个临时表之间的结构差异，包括数据长度、创建表的SQL语句、列的信息等
     * 主要用于检测表结构的变更，如添加、修改或删除列
     *
     * @param tableMetadata Doris表的元数据，包含表的基本信息如表名、schema等
     * @return 返回一个DorisCompareTableInfo对象，其中包含了表结构比较的结果，如添加、修改、删除的列信息等
     */
    @Override
    public @NonNull DorisCompareTableInfo compareTable(DorisTableMetadata tableMetadata) {
        // 获取表名
        String tableName = tableMetadata.getTableName();
        // 执行SQL查询获取表的数据长度
        Long tableDataLength = mapper.findTableDataLength(tableName);
        // 执行SQL查询获取创建表的SQL语句
        String createTableSql = mapper.findTableCreateSql(tableName);
        // 执行SQL查询获取表的列信息
        List<InformationSchemaColumn> columns = mapper.findTableEnsembleByTableName(tableName);
        // 加载临时表的信息
        DorisCompareTableInfo.TempTableInfo tempTableInfo = loadTempTableInfo(tableMetadata);

        // 比较创建表的SQL语句，找出差异
        Map<String, List<String>> compareSqlStatements = DorisHelper.compareSqlStatements(createTableSql, tempTableInfo.getCreateTableSql());
        List<String> added = compareSqlStatements.get(DorisHelper.ADDED);
        List<String> removed = compareSqlStatements.get(DorisHelper.REMOVED);
        List<String> removed_matched = new ArrayList<>();
        // 对比获取修改的列
        List<String> modified = new ArrayList<>();
        for (String addedLine : added) {
            // 正则表达式
            String regex = "`([^`]+)`";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(addedLine);
            String columnName = matcher.find() ? matcher.group(1) : "";
            if (columnName.isEmpty()) {
                continue;
            }
            if (!addedLine.startsWith("`" + columnName + "`")) {
                continue;
            }
            String matched = removed.stream()
                    .filter(line -> line.startsWith("`" + columnName + "`"))
                    .findFirst()
                    .orElse("");
            if (matched.isEmpty()) {
                continue;
            }
            modified.add(addedLine);
            removed_matched.add(matched);
        }
        added = added.stream()
                .filter(it -> !modified.contains(it))
                .collect(Collectors.toList());
        removed = removed.stream()
                .filter(it -> !removed_matched.contains(it))
                .collect(Collectors.toList());
        // 创建DorisCompareTableInfo对象，用于存储比较结果
        DorisCompareTableInfo compareTableInfo = new DorisCompareTableInfo(tableName, tableMetadata.getSchema());
        compareTableInfo.setTableDataLength(tableDataLength);
        compareTableInfo.setCreateTableSql(createTableSql);
        compareTableInfo.setColumns(columns);
        compareTableInfo.setTempTableInfo(tempTableInfo);
        compareTableInfo.setAdded(added);
        compareTableInfo.setModified(modified);
        compareTableInfo.setRemoved(removed);
        // 返回比较结果
        return compareTableInfo;
    }


    /**
     * 根据比较结果修改Doris表结构
     * 此方法用于根据新旧表信息对比结果来更新Doris中的表结构
     * 它首先检查新表数据量是否超过预设阈值，如果超过则不进行更新
     * 否则，将创建一个临时表来应用新的表结构，并在更新前选择性地备份旧表
     *
     * @param compareTableInfo 包含新旧表信息及比较结果的对象
     * @return 一个包含所有更新SQL语句的列表
     */
    @Override
    public List<String> modifyTable(DorisCompareTableInfo compareTableInfo) {
        // 获取AutoTable全局配置中关于Doris的更新限制设置
        long updateLimitTableDataLength = AutoTableGlobalConfig.getAutoTableProperties().getDoris().getUpdateLimitTableDataLength();
        boolean updateBackupOldTable = AutoTableGlobalConfig.getAutoTableProperties().getDoris().isUpdateBackupOldTable();

        // 获取当前时间，用于生成临时表名和备份表名
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String tableName = compareTableInfo.getName();
        String tempTableName = String.format("%s_temp_%s", tableName, now);

        // 如果新表数据量超过更新阈值，则记录警告信息并不进行更新操作
        if (compareTableInfo.getTableDataLength() > updateLimitTableDataLength) {
            log.warn("表{}数据量{}大于配置的更新阈值{}，将不进行更新操作", tableName, compareTableInfo.getTableDataLength(), updateLimitTableDataLength);
            return new ArrayList<>();
        }

        // 初始化SQL列表，用于存储所有更新操作的SQL语句
        List<String> sqlList = new ArrayList<>();

        // 创建临时表，用于应用新的表结构
        String createTempTable = compareTableInfo.getTempTableInfo()
                .getCreateTableSql()
                .replace("`" + tableName + "`", "`" + tempTableName + "`");
        sqlList.add(createTempTable);

        // 获取新表的列名集合
        Set<String> newColumns = compareTableInfo.getTempTableInfo()
                .getColumns()
                .stream()
                .map(InformationSchemaColumn::getColumnName)
                .collect(Collectors.toSet());

        // 获取旧表中需要插入到新表的列名列表
        List<String> insertColumns = compareTableInfo.getColumns()
                .stream()
                .map(InformationSchemaColumn::getColumnName)
                .filter(newColumns::contains)
                .collect(Collectors.toList());

        // 生成插入语句，将旧表数据插入到新表中
        String insertSelectSql = String.format("insert into `%s` (%s) select %s from `%s`",
                tempTableName,
                DorisHelper.joinColumns(insertColumns),
                DorisHelper.joinColumns(insertColumns),
                tableName);
        sqlList.add(insertSelectSql);

        // 根据配置决定是否备份旧表
        if (updateBackupOldTable) {
            sqlList.add(String.format("alter table `%s` rename `%s_bak_%s` ", tableName, tableName, now));
        } else {
            sqlList.add(String.format("drop table if exists `%s`", tableName));
        }

        // 将临时表重命名为正式表名，完成表结构更新
        sqlList.add(String.format("alter table `%s` rename `%s`", tempTableName, tableName));

        // 返回包含所有更新SQL的列表
        return sqlList;
    }

    /**
     * 加载临时表信息
     * 该方法用于根据给定的表元数据创建一个临时表，获取其结构信息，然后删除临时表
     * 主要用于在不改变原始表的情况下，比较或验证表结构
     *
     * @param tableMetadata 表元数据，包含表名和架构信息
     * @return 返回一个包含临时表创建SQL和列信息的对象
     */
    private DorisCompareTableInfo.TempTableInfo loadTempTableInfo(DorisTableMetadata tableMetadata) {
        // 获取表名和架构
        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();

        // 生成一个基于当前时间的唯一后缀，用于创建临时表名
        String tempTableName = String.format("%s_temp_%s", tableName, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        // 通过Sql构建器生成创建临时表的SQL语句
        String createTempTable = DorisSqlBuilder.buildSql(tableMetadata).replace("`" + tableName + "`", "`" + tempTableName + "`");

        // 执行SQL操作并返回结果
        try {
            // 执行创建临时表的SQL语句
            mapper.executeRawSql(createTempTable);

            // 获取临时表的创建SQL语句
            String showCreateTable = mapper.findTableCreateSql(tempTableName);
            String createTempTableSql = showCreateTable.replace("`" + tempTableName + "`", "`" + tableName + "`");

            // 获取临时表的列信息
            List<InformationSchemaColumn> columns = mapper.findTableEnsembleByTableName(tempTableName);

            // 返回包含临时表创建SQL和列信息的对象
            return new DorisCompareTableInfo.TempTableInfo(createTempTableSql, columns);
        } finally {
            // 生成并执行删除临时表的SQL语句
            String dropTable = dropTable(schema, tempTableName);
            mapper.executeRawSql(dropTable);
        }
    }
}
