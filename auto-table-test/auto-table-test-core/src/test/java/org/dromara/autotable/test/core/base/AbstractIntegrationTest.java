package org.dromara.autotable.test.core.base;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.Version;

/**
 * AutoTable 集成测试基类，提供常见测试场景的快捷方法
 */
public abstract class AbstractIntegrationTest extends AbstractAutoTableTest {

    /**
     * 设置运行模式
     *
     * @param mode 运行模式
     */
    protected void setMode(RunMode mode) {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(mode);
    }

    /**
     * 设置扫描包路径
     *
     * @param packages 包路径数组
     */
    protected void setModelPackage(String... packages) {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(packages);
    }

    /**
     * 设置扫描类
     *
     * @param classes 类数组
     */
    protected void setModelClass(Class<?>... classes) {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(classes);
    }

    /**
     * 启用自动删除列
     */
    protected void enableAutoDropColumn() {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setAutoDropColumn(true);
    }

    /**
     * 启用记录 SQL 功能（文件模式）
     *
     * @param folderPath SQL 记录文件夹路径
     */
    protected void enableRecordSqlFile(String folderPath) {
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.file);
        recordSqlProperties.setFolderPath(folderPath);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setRecordSql(recordSqlProperties);
    }

    /**
     * 启用记录 SQL 功能（Flyway 模式）
     *
     * @param folderPath SQL 记录文件夹路径
     */
    protected void enableRecordSqlFlyway(String folderPath) {
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.custom);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setRecordSql(recordSqlProperties);
        AutoTableGlobalConfig.instance().setCustomRecordSqlHandler(new org.dromara.autotable.test.core.RecordSqlFlywayHandler(folderPath));
    }

    /**
     * 启用记录 SQL 功能（数据库模式）
     *
     * @param tableName 记录表名
     */
    protected void enableRecordSqlDb(String tableName) {
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.db);
        recordSqlProperties.setTableName(tableName);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setRecordSql(recordSqlProperties);
    }

    /**
     * 配置 MySQL 特有属性：alter table 分离 drop 语句
     *
     * @param separate 是否分离
     */
    protected void setMysqlAlterTableSeparateDrop(boolean separate) {
        AutoTableGlobalConfig.instance().getAutoTableProperties().getMysql().setAlterTableSeparateDrop(separate);
    }

    /**
     * 设置父类字段插入位置
     *
     * @param position 位置
     */
    protected void setSuperInsertPosition(PropertyConfig.SuperInsertPosition position) {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setSuperInsertPosition(position);
    }

    /**
     * 启用自动建库
     */
    protected void enableAutoBuildDatabase() {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setAutoBuildDatabase(true);
    }
}
