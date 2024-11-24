package org.dromara.autotable.core.recordsql;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DatasourceNameManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class RecordSqlDbHandler implements RecordSqlHandler {
    @Override
    public void record(AutoTableExecuteSqlLog autoTableExecuteSqlLog) {

        PropertyConfig.RecordSqlProperties recordSqlConfig = AutoTableGlobalConfig.getAutoTableProperties().getRecordSql();

        // 优先使用自定义的表名，没有则根据统一的风格定义表名
        String tableName = recordSqlConfig.getTableName();
        if (StringUtils.noText(tableName)) {
            tableName = TableBeanUtils.getTableName(AutoTableExecuteSqlLog.class);
        }

        // 判断表是否存在，不存在则创建
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryManager.getSqlSessionFactory();
        try (SqlSession sqlSession = sqlSessionFactory.openSession();
             Connection connection = sqlSession.getConnection()) {
            String catalog = connection.getCatalog();
            String schema = StringUtils.hasText(autoTableExecuteSqlLog.getTableSchema()) ? autoTableExecuteSqlLog.getTableSchema() : connection.getSchema();
            boolean tableNotExit = !connection.getMetaData().getTables(catalog, schema, tableName, new String[]{"TABLE"}).next();
            connection.setAutoCommit(false);
            if (tableNotExit) {
                // 初始化表
                initTable(connection);
                log.info("初始化sql记录表：{}", tableName);
            }
            // 插入数据
            insertLog(tableName, autoTableExecuteSqlLog, connection);
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertLog(String tableName, AutoTableExecuteSqlLog autoTableExecuteSqlLog, Connection connection) throws SQLException {
        /* 插入数据 */
        Class<AutoTableExecuteSqlLog> sqlLogClass = AutoTableExecuteSqlLog.class;
        // 筛选列
        List<Field> columnFields = Arrays.stream(sqlLogClass.getDeclaredFields())
                .filter(field -> TableBeanUtils.isIncludeField(field, sqlLogClass))
                .collect(Collectors.toList());
        // 根据统一的风格定义列名
        List<String> columns = columnFields.stream()
                .map(field -> TableBeanUtils.getRealColumnName(sqlLogClass, field))
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
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, String.join(", ", columns), IntStream.range(0, values.size()).mapToObj(i -> "?").collect(Collectors.joining(", ")));
        log.info("插入SQL记录：{}", insertSql);
        PreparedStatement preparedStatement = connection.prepareStatement(insertSql);
        for (int i = 0; i < values.size(); i++) {
            preparedStatement.setObject(i + 1, values.get(i));
        }
        preparedStatement.executeUpdate();
    }

    private static void initTable(Connection connection) throws SQLException {

        IDataSourceHandler datasourceHandler = AutoTableGlobalConfig.getDatasourceHandler();
        String datasourceName = DatasourceNameManager.getDatasourceName();
        String databaseDialect = datasourceHandler.getDatabaseDialect(datasourceName);

        IStrategy<?, ?, ?> createTableStrategy = AutoTableGlobalConfig.getStrategy(databaseDialect);
        List<String> initTableSql = createTableStrategy.createTable(AutoTableExecuteSqlLog.class);

        try (Statement statement = connection.createStatement()) {
            for (String sql : initTableSql) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("初始化sql记录表失败", e);
        }
    }
}
