package org.dromara.autotable.core.strategy.oracle;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.Utils;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.recordsql.AutoTableExecuteSqlLog;
import org.dromara.autotable.core.recordsql.RecordSqlService;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.oracle.builder.CompareTableBuilder;
import org.dromara.autotable.core.strategy.oracle.builder.CreateTableSqlBuilder;
import org.dromara.autotable.core.strategy.oracle.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.core.strategy.oracle.builder.TableMetadataBuilder;
import org.dromara.autotable.core.strategy.oracle.data.OracleCompareTableInfo;
import org.dromara.autotable.core.strategy.oracle.data.OracleDefaultTypeEnum;
import org.dromara.autotable.core.strategy.oracle.data.OracleTableMetadata;
import org.dromara.autotable.core.strategy.oracle.mapper.OracleTablesMapper;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * Oracle适配器
 * @author KThirty
 * @since 2025/5/22 15:35
 */
@Slf4j
public class OracleStrategy implements IStrategy<OracleTableMetadata, OracleCompareTableInfo> {
    private final OracleTablesMapper mapper = new OracleTablesMapper();
    @Override
    public String databaseDialect() {return DatabaseDialect.Oracle;}


    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return new java.util.HashMap<Class<?>, DefaultTypeEnumInterface>(16) {{
            put(String.class, OracleDefaultTypeEnum.VARCHAR2);
            put(Character.class, OracleDefaultTypeEnum.CHAR);
            put(char.class, OracleDefaultTypeEnum.CHAR);

            put(java.math.BigInteger.class, OracleDefaultTypeEnum.NUMBER);
            put(Long.class, OracleDefaultTypeEnum.NUMBER);
            put(long.class, OracleDefaultTypeEnum.NUMBER);

            put(Integer.class, OracleDefaultTypeEnum.NUMBER);
            put(int.class, OracleDefaultTypeEnum.NUMBER);

            put(Boolean.class, OracleDefaultTypeEnum.BOOLEAN);
            put(boolean.class, OracleDefaultTypeEnum.BOOLEAN);

            put(Float.class, OracleDefaultTypeEnum.FLOAT);
            put(float.class, OracleDefaultTypeEnum.FLOAT);
            put(Double.class, OracleDefaultTypeEnum.FLOAT);
            put(double.class, OracleDefaultTypeEnum.FLOAT);
            put(java.math.BigDecimal.class, OracleDefaultTypeEnum.NUMBER);

            put(java.util.Date.class, OracleDefaultTypeEnum.DATE);
            put(java.sql.Date.class, OracleDefaultTypeEnum.DATE);
            put(java.sql.Timestamp.class, OracleDefaultTypeEnum.TIMESTAMP);
            put(java.sql.Time.class, OracleDefaultTypeEnum.DATE);
            put(java.time.LocalDateTime.class, OracleDefaultTypeEnum.TIMESTAMP);
            put(java.time.LocalDate.class, OracleDefaultTypeEnum.DATE);
            put(java.time.LocalTime.class, OracleDefaultTypeEnum.DATE);

            put(Short.class, OracleDefaultTypeEnum.NUMBER);
            put(short.class, OracleDefaultTypeEnum.NUMBER);
        }};
    }

    @Override
    public void createMode(OracleTableMetadata tableMetadata) {
        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();
        if (!checkTableNotExist(schema, tableName)) {
            // 表是否存在的标记
            log.info("create模式，删除表：{}", tableName);
            // 直接尝试删除表
            String sql = this.dropTable(schema, tableName);
            this.executeSql(tableMetadata, Collections.singletonList(sql));
        }
        // 新建表
        executeCreateTable(tableMetadata);
    }

    @Override
    public String dropTable(String schema, String tableName) {
        return String.format("DROP TABLE %s", tableName);
    }

    @Override
    public @NonNull OracleTableMetadata analyseClass(Class<?> beanClass) {
        return TableMetadataBuilder.build(beanClass);
    }

    @Override
    public List<String> createTable(OracleTableMetadata tableMetadata) {
        return CreateTableSqlBuilder.buildSql(tableMetadata);
    }

    @Override
    public @NonNull OracleCompareTableInfo compareTable(OracleTableMetadata tableMetadata) {
        return CompareTableBuilder.build(tableMetadata);
    }

    @Override
    public List<String> modifyTable(OracleCompareTableInfo compareTableInfo) {
        return ModifyTableSqlBuilder.buildSql(compareTableInfo);
    }

    @Override
    public void executeSql(OracleTableMetadata tableMetadata, List<String> sqlList) {
        List<AutoTableExecuteSqlLog> autoTableExecuteSqlLogs = new ArrayList<>();
        DataSourceManager.useConnection(connection -> {
            try {
                // 批量的SQL 改为手动提交模式
                connection.setAutoCommit(false);

                try (Statement statement = connection.createStatement()) {
                    boolean recordSql = AutoTableGlobalConfig.getAutoTableProperties().getRecordSql().isEnable();
                    for (String sql : sqlList) {
                        long executionTime = System.currentTimeMillis();
                        log.debug("执行Sql {}",sql);
                        statement.execute(sql);
                        long executionEndTime = System.currentTimeMillis();
                        if (recordSql) {
                            AutoTableExecuteSqlLog autoTableExecuteSqlLog = AutoTableExecuteSqlLog.of(tableMetadata.getEntityClass(), tableMetadata.getSchema(), tableMetadata.getTableName(), sql, executionTime, executionEndTime);
                            autoTableExecuteSqlLogs.add(autoTableExecuteSqlLog);
                        }

                        log.info("执行sql({}ms)：{}", executionEndTime - executionTime, sql);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(String.format("执行SQL期间出错: \n%s\n", String.join("\n", sqlList)), e);
                }
                // 提交
                connection.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            // 记录SQL
            if (!autoTableExecuteSqlLogs.isEmpty()) {
                RecordSqlService.record(autoTableExecuteSqlLogs);
            }
        });
    }
}
