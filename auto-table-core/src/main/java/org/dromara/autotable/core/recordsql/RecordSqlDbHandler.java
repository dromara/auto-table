package org.dromara.autotable.core.recordsql;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.dromara.autotable.core.utils.BeanClassUtil;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class RecordSqlDbHandler implements RecordSqlHandler {
    @Override
    public void record(List<AutoTableExecuteSqlLog> autoTableExecuteSqlLogs) {

        PropertyConfig.RecordSqlProperties recordSqlConfig = AutoTableGlobalConfig.instance().getAutoTableProperties().getRecordSql();

        // 优先使用自定义的表名，没有则根据统一的风格定义表名
        String tableName = recordSqlConfig.getTableName();
        if (StringUtils.noText(tableName)) {
            tableName = TableMetadataHandler.getTableName(AutoTableExecuteSqlLog.class);
        }

        // 判断表是否存在，不存在则创建
        String finalTableName = tableName;
        DataSourceManager.useConnection(connection -> {
            log.debug("开启sql记录事务");

            try {
                connection.setAutoCommit(false);
                for (AutoTableExecuteSqlLog autoTableExecuteSqlLog : autoTableExecuteSqlLogs) {
                    String schema = autoTableExecuteSqlLog.getTableSchema();

                    // 从线程上下文获取数据库策略
                    IStrategy<?, ?> createTableStrategy = IStrategy.getCurrentStrategy();
                    String schemaTableName = createTableStrategy.concatWrapName(schema, finalTableName);
                    if (createTableStrategy.checkTableNotExist(schema, finalTableName)) {
                        // 初始化表
                        initTable(createTableStrategy, connection, schema, finalTableName);
                        log.info("初始化sql记录表：{}", schemaTableName);
                    }
                    // 插入数据
                    insertLog(schemaTableName, autoTableExecuteSqlLog, connection);
                }
                log.debug("提交sql记录事务");
                connection.commit();
            } catch (Exception e) {
                throw new RuntimeException("记录sql到数据库出错", e);
            }
        });
    }

    private static void insertLog(String tableName, AutoTableExecuteSqlLog autoTableExecuteSqlLog, Connection connection) throws SQLException {
        /* 插入数据 */
        Class<AutoTableExecuteSqlLog> sqlLogClass = AutoTableExecuteSqlLog.class;
        // 筛选列
        List<Field> columnFields = Arrays.stream(sqlLogClass.getDeclaredFields())
                .filter(field -> TableMetadataHandler.isIncludeField(field, sqlLogClass))
                .collect(Collectors.toList());
        // 根据统一的风格定义列名
        List<String> columns = columnFields.stream()
                .map(field -> TableMetadataHandler.getColumnName(sqlLogClass, field))
                .map(IStrategy::wrapIdentifiers)
                .collect(Collectors.toList());
        // 获取每一列的值
        List<Object> values = columnFields.stream().map(field -> {
            try {
                field.setAccessible(true);
                return field.get(autoTableExecuteSqlLog);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        // 执行数据插入
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                String.join(", ", columns),
                IntStream.range(0, values.size()).mapToObj(i -> "?").collect(Collectors.joining(", ")));
        log.info("插入SQL记录：{}", insertSql);
        PreparedStatement preparedStatement = connection.prepareStatement(insertSql);
        for (int i = 0; i < values.size(); i++) {
            preparedStatement.setObject(i + 1, values.get(i));
        }
        preparedStatement.executeUpdate();
    }

    private static void initTable(IStrategy<?, ?> createTableStrategy, Connection connection, String schema, String customTableName) throws SQLException {

        // 使用相应的策略创建数据库表
        List<String> initTableSql = createTableStrategy.createTable(AutoTableExecuteSqlLog.class, tableMetadata -> {
            if (!Objects.equals(customTableName, tableMetadata.getTableName())) {

                try {
                    // 自定义SQL记录表的名称
                    Field tableNameField = BeanClassUtil.getField(tableMetadata.getClass(), TableMetadata.tableNameFieldName);
                    tableNameField.setAccessible(true);
                    tableNameField.set(tableMetadata, customTableName);
                    // 自定义schema
                    Field schemaField = BeanClassUtil.getField(tableMetadata.getClass(), TableMetadata.schemaFieldName);
                    schemaField.setAccessible(true);
                    schemaField.set(tableMetadata, schema);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return tableMetadata;
        });


        try (Statement statement = connection.createStatement()) {
            for (String sql : initTableSql) {
                log.debug("执行sql：{}", sql);
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("初始化sql记录表失败", e);
        }

    }
}
