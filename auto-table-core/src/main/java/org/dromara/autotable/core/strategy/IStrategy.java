package org.dromara.autotable.core.strategy;

import lombok.NonNull;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.Utils;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.initdata.InitDataHandler;
import org.dromara.autotable.core.recordsql.AutoTableExecuteSqlLog;
import org.dromara.autotable.core.recordsql.RecordSqlService;
import org.dromara.autotable.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author don
 */
public interface IStrategy<TABLE_META extends TableMetadata, COMPARE_TABLE_INFO extends CompareTableInfo> {

    Logger log = LoggerFactory.getLogger(IStrategy.class);

    /**
     * 当前运行的数据库策略
     */
    ThreadLocal<IStrategy<?, ?>> STRATEGY_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的策略
     *
     * @param dataSource 数据源
     */
    static void setCurrentStrategy(@NonNull IStrategy<?, ?> dataSource) {
        STRATEGY_THREAD_LOCAL.set(dataSource);
    }

    /**
     * 获取当前线程的策略
     *
     * @return 当前线程的策略
     */
    static IStrategy<?, ?> getCurrentStrategy() {
        IStrategy<?, ?> iStrategy = STRATEGY_THREAD_LOCAL.get();
        if (iStrategy == null) {
            throw new RuntimeException("当前线程没有设置IStrategy");
        }
        return iStrategy;
    }

    /**
     * 清理当前线程的策略
     */
    static void clean() {
        STRATEGY_THREAD_LOCAL.remove();
    }

    /**
     * 使用数据库标识符包裹名称并根据指定连字符链接
     *
     * @param hyphen 连字符
     * @param names  名称
     */
    static String customConcatWrapIdentifiers(String hyphen, Collection<String> names) {
        return names.stream()
                .filter(StringUtils::hasText)
                .map(IStrategy::wrapIdentifiers)
                .collect(Collectors.joining(hyphen));
    }

    /**
     * 使用数据库标识符包裹名称
     *
     * @param name 名称
     * @return 带有标识符的名称
     */
    static String wrapIdentifiers(String name) {
        return IStrategy.getCurrentStrategy().wrapIdentifier(name);
    }

    /**
     * 获取数据库标识符包裹后的名称
     *
     * @param names 名称
     * @return 带有schema前缀后的名称
     */
    static String concatWrapIdentifiers(String... names) {
        return IStrategy.getCurrentStrategy().concatWrapName(names);
    }

    /**
     * sql包装，如果sql以分号结尾，则不添加分号，否则添加分号
     *
     * @param rawSql 原始sql
     * @return 包装后的sql
     */
    default String wrapSql(String rawSql) {
        String trimmed = rawSql.trim();
        if (!trimmed.endsWith(";")) {
            return trimmed + ";";
        }
        return trimmed;
    }

    /**
     * 数据库标识符引号
     * <p>MySQL 反引号 `
     * <p>PostgreSQL 双引号 "
     * <p>SQL Server 方括号 [ 和 ]
     * <p>Oracle 双引号 "
     * <p>SQLite 支持 "、\``、[]`
     */
    default String identifier() {
        return "\"";
    }

    /**
     * 使用数据库标识符包裹名称
     *
     * @param name 名称
     * @return 带有标识符的名称
     */
    default String wrapIdentifier(String name) {
        String identifier = identifier();
        if (!name.startsWith(identifier) || !name.endsWith(identifier)) {
            return identifier + name + identifier;
        }
        return name;
    }

    /**
     * 链接schema、表、索引 等名称，并使用数据库标识符包裹
     *
     * @param names 名称（表名或者索引名）
     */
    default String concatWrapName(String... names) {

        return Arrays.stream(names)
                .filter(StringUtils::hasText)
                .map(this::wrapIdentifier)
                .collect(Collectors.joining("."));
    }

    /**
     * 索引名称最大长度: 考虑到大多数数据库，其中oracle的30最小，再就是pg的63了，所以这里取63，oracle自行处理
     *
     * @return 索引名称最大长度
     */
    default int indexNameMaxLength() {
        return 63;
    }

    /**
     * 开始分析实体集合
     *
     * @param entityClass 待处理的实体
     */
    default TABLE_META start(Class<?> entityClass) {

        AutoTableGlobalConfig.instance().getRunBeforeCallbacks().forEach(fn -> fn.before(entityClass));

        TABLE_META tableMetadata = this.analyseClass(entityClass);

        this.start(tableMetadata);

        AutoTableGlobalConfig.instance().getRunAfterCallbacks().forEach(fn -> fn.after(entityClass));

        return tableMetadata;
    }

    /**
     * 开始分析实体
     *
     * @param tableMetadata 表元数据
     */
    default void start(TABLE_META tableMetadata) {
        // 拦截表信息，供用户自定义修改
        AutoTableGlobalConfig.instance().getBuildTableMetadataInterceptors().forEach(fn -> fn.intercept(this.databaseDialect(), tableMetadata));

        RunMode runMode = AutoTableGlobalConfig.instance().getAutoTableProperties().getMode();

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
            AutoTableGlobalConfig.instance().getValidateFinishCallbacks().forEach(fn -> fn.validateFinish(false, this.databaseDialect(), null));
            throw new RuntimeException(String.format("启动失败，%s中不存在表%s", this.databaseDialect(), tableMetadata.getTableName()));
        }

        // 对比数据库表结构与新的表元数据的差异
        COMPARE_TABLE_INFO compareTableInfo = this.compareTable(tableMetadata);
        if (compareTableInfo.needModify()) {
            log.warn("{}表结构不一致：\n{}", tableMetadata.getTableName(), compareTableInfo.validateFailedMessage());
            AutoTableGlobalConfig.instance().getValidateFinishCallbacks().forEach(fn -> fn.validateFinish(false, this.databaseDialect(), compareTableInfo));
            throw new RuntimeException(String.format("启动失败，%s数据表%s与实体不匹配", this.databaseDialect(), tableMetadata.getTableName()));
        }
        AutoTableGlobalConfig.instance().getValidateFinishCallbacks().forEach(fn -> fn.validateFinish(true, this.databaseDialect(), compareTableInfo));
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
        this.createSchema(schema);

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
        this.createSchema(schema);

        String tableName = tableMetadata.getTableName();

        boolean tableNotExist = this.checkTableNotExist(schema, tableName);
        // 当表不存在的时候，直接创建表
        if (tableNotExist) {
            executeCreateTable(tableMetadata);
            return;
        }

        // 当表存在，比对数据库表结构与表元数据的差异
        COMPARE_TABLE_INFO compareTableInfo = this.compareTable(tableMetadata);
        AutoTableGlobalConfig.instance().getCompareTableFinishCallbacks().forEach(fn -> fn.afterCompareTable(this.databaseDialect(), tableMetadata, compareTableInfo));
        if (compareTableInfo.needModify()) {
            // 修改表信息
            log.info("修改表：{}", (StringUtils.hasText(schema) ? schema + "." : "") + tableName);
            AutoTableGlobalConfig.instance().getModifyTableInterceptors().forEach(fn -> fn.beforeModifyTable(this.databaseDialect(), tableMetadata, compareTableInfo));
            List<String> sqlList = this.modifyTable(compareTableInfo);
            this.executeSql(tableMetadata, sqlList);
            AutoTableGlobalConfig.instance().getModifyTableFinishCallbacks().forEach(fn -> fn.afterModifyTable(this.databaseDialect(), tableMetadata, compareTableInfo));
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

        AutoTableGlobalConfig.instance().getCreateTableInterceptors().forEach(fn -> fn.beforeCreateTable(this.databaseDialect(), tableMetadata));
        List<String> sqlList = this.createTable(tableMetadata);
        this.executeSql(tableMetadata, sqlList);
        // 建表完成，执行表的sql初始化
        InitDataHandler.initTableData(tableMetadata);

        AutoTableGlobalConfig.instance().getCreateTableFinishCallbacks().forEach(fn -> fn.afterCreateTable(this.databaseDialect(), tableMetadata));
    }

    /**
     * 执行SQL
     *
     * @param tableMetadata 表元数据
     * @param sqlList       SQL集合
     */
    default void executeSql(TABLE_META tableMetadata, List<String> sqlList) {

        List<AutoTableExecuteSqlLog> autoTableExecuteSqlLogs = new ArrayList<>();

        DataSourceManager.useConnection(connection -> {
            try {
                // 批量的SQL 改为手动提交模式
                connection.setAutoCommit(false);

                try (Statement statement = connection.createStatement()) {
                    boolean recordSql = AutoTableGlobalConfig.instance().getAutoTableProperties().getRecordSql().isEnable();
                    for (String sql : sqlList) {
                        // sql包装
                        sql = wrapSql(sql);

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
                    throw new RuntimeException(String.format("执行SQL期间出错: \n%s\n", java.lang.String.join("\n", sqlList)), e);
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

    /**
     * 检查表是否存在
     *
     * @param schema    schema
     * @param tableName 表名
     * @return 表详情
     */
    default boolean checkTableNotExist(String schema, String tableName) {
        List<String> tables = listAllTables(schema);
        boolean exist = tables.stream().anyMatch(name -> name.equalsIgnoreCase(tableName));
        return !exist;
    }

    /**
     * 查询所有表
     *
     * @return 表名集合
     */
    default List<String> listAllTables(String schema) {
        return DataSourceManager.useConnection(connection -> {
            try {
                return Utils.getTables(connection, schema, new String[]{"TABLE"});
            } catch (SQLException e) {
                throw new RuntimeException("查询所有表出错", e);
            }
        });
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
     * 分析Bean，得到元数据信息
     *
     * @param beanClass 待分析的class
     * @return 表元信息
     */
    @NonNull
    TABLE_META analyseClass(Class<?> beanClass);

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
     * 创建schema
     *
     * @param schema schema名字
     */
    default void createSchema(String schema) {
    }

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
