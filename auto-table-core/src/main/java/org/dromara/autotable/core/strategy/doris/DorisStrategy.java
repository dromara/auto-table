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
import org.dromara.autotable.core.strategy.doris.data.DorisTableMetadata;
import org.dromara.autotable.core.strategy.doris.data.enums.DorisDefaultTypeEnum;
import org.dromara.autotable.core.strategy.doris.mapper.DorisTablesMapper;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaColumn;

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
public class DorisStrategy implements IStrategy<DorisTableMetadata, DorisCompareTableInfo, DorisTablesMapper> {
    public static final String databaseDialect = DatabaseDialect.Doris;

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

            put(Boolean.class, DorisDefaultTypeEnum.BIT);
            put(boolean.class, DorisDefaultTypeEnum.BIT);

            put(Float.class, DorisDefaultTypeEnum.FLOAT);
            put(float.class, DorisDefaultTypeEnum.FLOAT);
            put(Double.class, DorisDefaultTypeEnum.DOUBLE);
            put(double.class, DorisDefaultTypeEnum.DOUBLE);
            put(BigDecimal.class, DorisDefaultTypeEnum.DECIMAL);

            put(Date.class, DorisDefaultTypeEnum.DATETIME);
            put(java.sql.Date.class, DorisDefaultTypeEnum.DATE);
            put(java.sql.Timestamp.class, DorisDefaultTypeEnum.DATETIME);
            put(java.sql.Time.class, DorisDefaultTypeEnum.TIME);
            put(LocalDateTime.class, DorisDefaultTypeEnum.DATETIME);
            put(LocalDate.class, DorisDefaultTypeEnum.DATE);
            put(LocalTime.class, DorisDefaultTypeEnum.TIME);

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

    @Override
    public @NonNull DorisCompareTableInfo compareTable(DorisTableMetadata tableMetadata) {
        String tableName = tableMetadata.getTableName();
        Long tableDataLength = executeReturn(mapper -> mapper.findTableDataLength(tableName));
        String createTableSql = executeReturn(mapper -> {
            Map<String, String> showCreateTable = mapper.findTableCreateSql(tableName);
            return showCreateTable.get("Create Table");
        });
        List<InformationSchemaColumn> columns = executeReturn(mapper -> mapper.findTableEnsembleByTableName(tableName));
        DorisCompareTableInfo.TempTableInfo tempTableInfo = loadTempTableInfo(tableMetadata);

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
        DorisCompareTableInfo compareTableInfo = new DorisCompareTableInfo(tableName, tableMetadata.getSchema());
        compareTableInfo.setTableDataLength(tableDataLength);
        compareTableInfo.setCreateTableSql(createTableSql);
        compareTableInfo.setColumns(columns);
        compareTableInfo.setTempTableInfo(tempTableInfo);
        compareTableInfo.setAdded(added);
        compareTableInfo.setModified(modified);
        compareTableInfo.setRemoved(removed);
        return compareTableInfo;
    }


    @Override
    public List<String> modifyTable(DorisCompareTableInfo compareTableInfo) {
        long updateLimitTableDataLength = AutoTableGlobalConfig.getAutoTableProperties().getDoris().getUpdateLimitTableDataLength();
        boolean updateBackupOldTable = AutoTableGlobalConfig.getAutoTableProperties().getDoris().isUpdateBackupOldTable();

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String tableName = compareTableInfo.getName();
        String tempTableName = String.format("%s_temp_%s", tableName, now);
        if (compareTableInfo.getTableDataLength() > updateLimitTableDataLength) {
            log.warn("表{}数据量{}大于配置的更新阈值{}，将不进行更新操作", tableName, compareTableInfo.getTableDataLength(), updateLimitTableDataLength);
            return new ArrayList<>();
        }
        List<String> sqlList = new ArrayList<>();
        String createTempTable = compareTableInfo.getTempTableInfo()
                .getCreateTableSql()
                .replace("`" + tableName + "`", "`" + tempTableName + "`");
        sqlList.add(createTempTable);

        Set<String> newColumns = compareTableInfo.getTempTableInfo()
                .getColumns()
                .stream()
                .map(InformationSchemaColumn::getColumnName)
                .collect(Collectors.toSet());
        List<String> insertColumns = compareTableInfo.getColumns()
                .stream()
                .map(InformationSchemaColumn::getColumnName)
                .filter(newColumns::contains)
                .collect(Collectors.toList());
        String insertSelectSql = String.format("insert into `%s` (%s) select %s from `%s`",
                tempTableName,
                DorisHelper.joinColumns(insertColumns),
                DorisHelper.joinColumns(insertColumns),
                tableName);
        sqlList.add(insertSelectSql);
        if (updateBackupOldTable) {
            sqlList.add(String.format("alter table `%s` rename `%s_bak_%s` ", tableName, tableName, now));
        } else {
            sqlList.add(String.format("drop table if exists `%s`", tableName));
        }
        sqlList.add(String.format("alter table `%s` rename `%s`", tempTableName, tableName));
        return sqlList;
    }

    private DorisCompareTableInfo.TempTableInfo loadTempTableInfo(DorisTableMetadata tableMetadata) {
        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();
        String tempTableName = String.format("%s_temp_%s", tableName, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        String createTempTable = DorisSqlBuilder.buildSql(tableMetadata).replace("`" + tableName + "`", "`" + tempTableName + "`");
        return executeReturn(mapper -> {
            try {
                mapper.executeRawSql(createTempTable);
                Map<String, String> showCreateTable = mapper.findTableCreateSql(tempTableName);
                String createTempTableSql = showCreateTable.get("Create Table")
                        .replace("`" + tempTableName + "`", "`" + tableName + "`");
                List<InformationSchemaColumn> columns = mapper.findTableEnsembleByTableName(tempTableName);
                return new DorisCompareTableInfo.TempTableInfo(createTempTableSql, columns);
            } finally {
                String dropTable = dropTable(schema, tempTableName);
                mapper.executeRawSql(dropTable);
            }
        });
    }
}
