package org.dromara.autotable.springboot.properties;

import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author don
 */
@Data
@ConfigurationProperties("auto-table")
@ConditionalOnMissingBean(AutoTableProperties.class)
public class AutoTableProperties {

    public static final String ENABLE_KEY = "auto-table.enable";

    /**
     * 是否显示banner
     */
    private Boolean showBanner = true;
    /**
     * 是否启用自动维护表功能
     */
    private Boolean enable = true;
    /**
     * 启动模式
     * none：系统不做任何处理。
     * create：系统启动后，会先将所有的表删除掉，然后根据model中配置的结构重新建表，该操作会破坏原有数据。
     * update：系统启动后，会自动判断哪些表是新建的，哪些字段要新增修改，哪些索引/约束要新增删除等，该操作不会删除字段(更改字段名称的情况下，会认为是新增字段)。
     * add：系统启动后，只做新增，比如新增表/新增字段/新增索引/新增唯一约束的功能，而不会去做修改和删除的操作。
     */
    private RunMode mode = RunMode.update;
    /**
     * 您的model包路径，多个路径可以用分号或者逗号隔开，会递归这个目录下的全部目录中的java对象，支持类似com.bz.**.entity
     * 缺省值：[Spring启动类所在包]
     */
    private String[] modelPackage = new String[]{};
    /**
     * 您的model类，多个可以用分号或者逗号隔开
     */
    private Class<?>[] modelClass = new Class[]{};
    /**
     * 自己定义的索引前缀
     */
    private String indexPrefix = "auto_idx_";
    /**
     * 自动删除名称不匹配的字段：强烈不建议开启，会发生丢失数据等不可逆的操作。
     */
    private Boolean autoDropColumn = false;
    /**
     * 是否自动删除名称不匹配的索引
     */
    private Boolean autoDropIndex = true;
    /**
     * 子类继承父类的字段的配置，是否开启严格继承的模式：只继承public、protected修饰的字段
     */
    private Boolean strictExtends = true;
    /**
     * <p>建表的时候，父类的字段排序是在子类后面还是前面
     * <p>默认为after，跟在子类的后面
     */
    private SuperInsertPosition superInsertPosition = SuperInsertPosition.after;

    /**
     * mysql配置
     */
    private Mysql mysql = new Mysql();

    /**
     * 记录执行的SQL
     */
    private RecordSqlProperties recordSql = new RecordSqlProperties();

    public PropertyConfig toConfig() {
        PropertyConfig propertyConfig = new PropertyConfig();
        propertyConfig.setShowBanner(this.showBanner);
        propertyConfig.setEnable(this.enable);
        propertyConfig.setMode(this.mode);
        propertyConfig.setModelPackage(this.modelPackage);
        propertyConfig.setModelClass(this.modelClass);
        propertyConfig.setIndexPrefix(this.indexPrefix);
        propertyConfig.setAutoDropColumn(this.autoDropColumn);
        propertyConfig.setAutoDropIndex(this.autoDropIndex);
        propertyConfig.setStrictExtends(this.strictExtends);

        PropertyConfig.SuperInsertPosition superInsertPosition =
                PropertyConfig.SuperInsertPosition.valueOf(this.superInsertPosition.name());
        propertyConfig.setSuperInsertPosition(superInsertPosition);

        PropertyConfig.MysqlConfig mysqlConfig = new PropertyConfig.MysqlConfig();
        mysqlConfig.setTableDefaultCharset(this.mysql.getTableDefaultCharset());
        mysqlConfig.setTableDefaultCharset(this.mysql.getTableDefaultCharset());
        mysqlConfig.setTableDefaultCharset(this.mysql.getTableDefaultCharset());
        mysqlConfig.setTableDefaultCharset(this.mysql.getTableDefaultCharset());
        propertyConfig.setMysql(mysqlConfig);

        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(this.recordSql.enable);
        recordSqlProperties.setTableName(this.recordSql.tableName);
        recordSqlProperties.setFolderPath(this.recordSql.folderPath);
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.valueOf(this.recordSql.recordType.name()));
        recordSqlProperties.setVersion(this.recordSql.version);
        propertyConfig.setRecordSql(recordSqlProperties);

        return propertyConfig;
    }

    @Data
    public static class Mysql {
        /**
         * 表默认字符集
         */
        private String tableDefaultCharset;
        /**
         * 表默认排序规则
         */
        private String tableDefaultCollation;
        /**
         * 列默认字符集
         */
        private String columnDefaultCharset;
        /**
         * 列默认排序规则
         */
        private String columnDefaultCollation;
    }

    @Data
    public static class RecordSqlProperties {
        /**
         * 开启记录sql日志
         */
        private boolean enable = false;

        /**
         * 记录方式，默认是数据库
         */
        private TypeEnum recordType = TypeEnum.db;

        /**
         * 当前SQL的版本，建议指定，会体现在数据库的字段或者文件名上
         */
        private String version;

        /**
         * 数据库记录方式下，表的名字
         */
        private String tableName;

        /**
         * 文件记录方式下，必须设置该值。 记录到文件的目录（目录不存在的情况下会自动创建），sql文件名会自动按照内置规则创建
         */
        private String folderPath;

        public static enum TypeEnum {
            /**
             * 记录到数据库
             */
            db,
            /**
             * 记录到文件
             */
            file,
            /**
             * 自定义
             */
            custom
        }
    }

    public enum SuperInsertPosition {
        /**
         * 在子类的后面
         */
        after,
        /**
         * 在子类的前面
         */
        before
    }
}
