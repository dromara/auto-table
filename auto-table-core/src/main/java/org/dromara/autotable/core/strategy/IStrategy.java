package org.dromara.autotable.core.strategy;

import lombok.NonNull;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.Utils;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.dromara.autotable.core.recordsql.AutoTableExecuteSqlLog;
import org.dromara.autotable.core.recordsql.RecordSqlService;
import org.dromara.autotable.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author don
 */
public interface IStrategy<TABLE_META extends TableMetadata, COMPARE_TABLE_INFO extends CompareTableInfo, MAPPER> {

    Logger log = LoggerFactory.getLogger(IStrategy.class);

    /**
     * 获取mapper执行mapper的方法
     *
     * @param execute 要执行的SQL方法
     * @return 数据库类型
     */
    default <R> R executeReturn(Function<MAPPER, R> execute) {

        // 从接口泛型上读取MapperClass
        Class<MAPPER> mapperClass = getMapperClass();

        // 执行
        try (SqlSession sqlSession = SqlSessionFactoryManager.getSqlSessionFactory().openSession()) {
            return execute.apply(sqlSession.getMapper(mapperClass));
        }
    }

    /**
     * 从接口泛型上读取MapperClass
     *
     * @return MapperClass
     */
    default Class<MAPPER> getMapperClass() {

        // 从接口泛型上读取MapperClass
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericInterfaces()[0];
        Class<MAPPER> mapperClass = (Class<MAPPER>) genericSuperclass.getActualTypeArguments()[2];

        // 如果没有注册Mapper，则注册
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryManager.getSqlSessionFactory();
        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (!configuration.hasMapper(mapperClass)) {
            configuration.addMapper(mapperClass);
        }

        return mapperClass;
    }

    /**
     * 开始分析实体集合
     *
     * @param entityClass 待处理的实体
     */
    default void start(Class<?> entityClass) {

        AutoTableGlobalConfig.getRunBeforeCallbacks().forEach(fn -> fn.before(entityClass));

        TABLE_META tableMetadata = this.analyseClass(entityClass);

        this.start(tableMetadata);

        AutoTableGlobalConfig.getRunAfterCallbacks().forEach(fn -> fn.after(entityClass));
    }

    /**
     * 开始分析实体
     *
     * @param tableMetadata 表元数据
     */
    default void start(TABLE_META tableMetadata) {
        // 拦截表信息，供用户自定义修改
        AutoTableGlobalConfig.getBuildTableMetadataInterceptors().forEach(fn -> fn.intercept(this.databaseDialect(), tableMetadata));

        RunMode runMode = AutoTableGlobalConfig.getAutoTableProperties().getMode();

        switch (runMode) {
            case validate:
                validateMode(tableMetadata);
                break;
            case create:
                createMode(tableMetadata);
                break;
            case update:
                updateMode(tableMetadata);
                break;
            default:
                throw new RuntimeException(String.format("不支持的运行模式：%s", runMode));
        }
    }

    /**
     * 检查数据库数据模型与实体是否一致
     * 1. 检查数据库数据模型是否存在
     * 2. 检查数据库数据模型与实体是否一致
     *
     * @param tableMetadata 表元数据
     */
    default void validateMode(TABLE_META tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        // 检查数据库数据模型与实体是否一致
        boolean tableNotExist = this.checkTableNotExist(schema, tableName);
        if (tableNotExist) {
            AutoTableGlobalConfig.getValidateFinishCallbacks().forEach(fn -> fn.validateFinish(false, this.databaseDialect(), null));
            throw new RuntimeException(String.format("启动失败，%s中不存在表%s", this.databaseDialect(), tableMetadata.getTableName()));
        }

        // 对比数据库表结构与新的表元数据的差异
        COMPARE_TABLE_INFO compareTableInfo = this.compareTable(tableMetadata);
        if (compareTableInfo.needModify()) {
            log.warn("{}表结构不一致：\n{}", tableMetadata.getTableName(), compareTableInfo.validateFailedMessage());
            AutoTableGlobalConfig.getValidateFinishCallbacks().forEach(fn -> fn.validateFinish(false, this.databaseDialect(), compareTableInfo));
            throw new RuntimeException(String.format("启动失败，%s数据表%s与实体不匹配", this.databaseDialect(), tableMetadata.getTableName()));
        }
        AutoTableGlobalConfig.getValidateFinishCallbacks().forEach(fn -> fn.validateFinish(true, this.databaseDialect(), compareTableInfo));
    }

    /**
     * 创建模式
     * <p>1. 删除表
     * <p>2. 新建表
     *
     * @param tableMetadata 表元数据
     */
    default void createMode(TABLE_META tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        // 表是否存在的标记
        log.info("create模式，删除表：{}", tableName);
        // 直接尝试删除表
        String sql = this.dropTable(schema, tableName);
        this.executeSql(tableMetadata, Collections.singletonList(sql));

        // 新建表
        executeCreateTable(tableMetadata);
    }

    /**
     * 更新模式
     * 1. 检查表是否存在
     * 2. 不存在创建
     * 3. 检查表是否需要修改
     * 4. 需要修改就修改表
     *
     * @param tableMetadata 表元数据
     */
    default void updateMode(TABLE_META tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        boolean tableNotExist = this.checkTableNotExist(schema, tableName);
        // 当表不存在的时候，直接创建表
        if (tableNotExist) {
            executeCreateTable(tableMetadata);
            return;
        }

        // 当表存在，比对数据库表结构与表元数据的差异
        COMPARE_TABLE_INFO compareTableInfo = this.compareTable(tableMetadata);
        if (compareTableInfo.needModify()) {
            // 修改表信息
            log.info("修改表：{}", (StringUtils.hasText(schema) ? schema + "." : "") + tableName);
            AutoTableGlobalConfig.getModifyTableInterceptors().forEach(fn -> fn.beforeModifyTable(this.databaseDialect(), tableMetadata, compareTableInfo));
            List<String> sqlList = this.modifyTable(compareTableInfo);
            this.executeSql(tableMetadata, sqlList);
            AutoTableGlobalConfig.getModifyTableFinishCallbacks().forEach(fn -> fn.afterModifyTable(this.databaseDialect(), tableMetadata, compareTableInfo));
        }
    }

    /**
     * 执行创建表
     *
     * @param tableMetadata 表元数据
     */
    default void executeCreateTable(TABLE_META tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();
        log.info("创建表：{}", (StringUtils.hasText(schema) ? schema + "." : "") + tableName);

        AutoTableGlobalConfig.getCreateTableInterceptors().forEach(fn -> fn.beforeCreateTable(this.databaseDialect(), tableMetadata));
        List<String> sqlList = this.createTable(tableMetadata);
        this.executeSql(tableMetadata, sqlList);
        AutoTableGlobalConfig.getCreateTableFinishCallbacks().forEach(fn -> fn.afterCreateTable(this.databaseDialect(), tableMetadata));
    }

    /**
     * 执行SQL
     *
     * @param tableMetadata 表元数据
     * @param sqlList       SQL集合
     */
    default void executeSql(TABLE_META tableMetadata, List<String> sqlList) {
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryManager.getSqlSessionFactory();

        List<AutoTableExecuteSqlLog> autoTableExecuteSqlLogs = new ArrayList<>();
        try (SqlSession sqlSession = sqlSessionFactory.openSession();
             Connection connection = sqlSession.getConnection()) {

            log.debug("开启执行sql事务");
            // 批量的SQL 改为手动提交模式
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                boolean recordSql = AutoTableGlobalConfig.getAutoTableProperties().getRecordSql().isEnable();
                for (String sql : sqlList) {
                    // sql末尾添加;
                    if (!sql.endsWith(";")) {
                        sql += ";";
                    }

                    long executionTime = System.currentTimeMillis();
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
            log.debug("提交执行sql事务");
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("获取数据库连接出错", e);
        } finally {
            log.debug("关闭执行sql事务");
        }

        // 记录SQL
        if (!autoTableExecuteSqlLogs.isEmpty()) {
            RecordSqlService.record(autoTableExecuteSqlLogs);
        }
    }

    /**
     * 检查表是否存在
     *
     * @param schema    schema
     * @param tableName 表名
     * @return 表详情
     */
    default boolean checkTableNotExist(String schema, String tableName) {
        // 获取Configuration对象
        Configuration configuration = SqlSessionFactoryManager.getSqlSessionFactory().getConfiguration();
        try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
            if (!StringUtils.hasText(schema)) {
                schema = connection.getSchema();
            }
            boolean exist = Utils.tableIsExists(connection, schema, tableName, new String[]{"TABLE"}, true);
            return !exist;
        } catch (SQLException e) {
            throw new RuntimeException("判断数据库是否存在出错", e);
        }
    }

    /**
     * 获取创建表的SQL
     *
     * @param clazz 实体
     * @return sql
     */
    default List<String> createTable(Class<?> clazz, Function<TABLE_META, TABLE_META> function) {
        TABLE_META tableMeta = this.analyseClass(clazz);
        if (function != null) {
            tableMeta = function.apply(tableMeta);
        }
        return this.createTable(tableMeta);
    }

    /**
     * 策略对应的数据库方言，与数据库驱动中的接口{@link java.sql.DatabaseMetaData#getDatabaseProductName()}实现返回值一致
     *
     * @return 方言
     */
    String databaseDialect();

    /**
     * java字段类型与数据库类型映射关系
     *
     * @return 映射
     */
    Map<Class<?>, DefaultTypeEnumInterface> typeMapping();

    /**
     * 根据表名删除表，生成删除表的SQL
     *
     * @param schema    schema
     * @param tableName 表名
     * @return SQL
     */
    String dropTable(String schema, String tableName);

    /**
     * 分析Bean，得到元数据信息
     *
     * @param beanClass 待分析的class
     * @return 表元信息
     */
    @NonNull
    TABLE_META analyseClass(Class<?> beanClass);

    /**
     * 生成创建表SQL
     *
     * @param tableMetadata 表元数据
     * @return SQL
     */
    List<String> createTable(TABLE_META tableMetadata);

    /**
     * 对比表与bean的差异
     *
     * @param tableMetadata 表元数据
     * @return 待修改的表信息描述
     */
    @NonNull
    COMPARE_TABLE_INFO compareTable(TABLE_META tableMetadata);

    /**
     * 生成修改表SQL
     *
     * @param compareTableInfo 修改表的描述信息
     * @return SQL
     */
    List<String> modifyTable(COMPARE_TABLE_INFO compareTableInfo);
}
