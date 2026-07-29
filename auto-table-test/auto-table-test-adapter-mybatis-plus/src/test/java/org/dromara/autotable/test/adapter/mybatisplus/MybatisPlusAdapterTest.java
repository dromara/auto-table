package org.dromara.autotable.test.adapter.mybatisplus;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.dromara.autotable.springboot.EnableAutoTableTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MP 适配器集成测试。
 * <p>
 * 验证 auto-table-adapter-mybatis-plus-spring-boot-starter 在 Spring Boot 环境下的 MP 原生注解兼容能力。
 * <p>
 * 本测试只验证 MP 原生注解（{@code @TableName}/{@code @TableField}/{@code @TableId}）的支持，
 * 自定义注解（{@code @Table}/{@code @Column}/{@code @ColumnId}）的测试属于 mybatis-plus-ext 项目。
 */
@EnableAutoTableTest
@SpringBootTest(classes = Application.class)
public class MybatisPlusAdapterTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext context;

    // ===== 自动装配验证 =====

    @Test
    public void testAutoConfigurationLoaded() {
        assertTrue(context.containsBean("mybatisPlusAdapterConfig"),
                "mybatisPlusAdapterConfig Bean 应被自动注册");
        assertTrue(context.containsBean("mybatisPlusMetadataAdapter"),
                "mybatisPlusMetadataAdapter Bean 应被自动注册");
        assertTrue(context.containsBean("mybatisPlusJavaTypeToDatabaseTypeConverter"),
                "mybatisPlusJavaTypeToDatabaseTypeConverter Bean 应被自动注册");
        assertTrue(context.containsBean("mybatisPlusRunBeforeCallback"),
                "mybatisPlusRunBeforeCallback Bean 应被自动注册");
        assertTrue(context.containsBean("mybatisPlusRunAfterCallback"),
                "mybatisPlusRunAfterCallback Bean 应被自动注册");
    }

    // ===== MP 原生注解建表验证 =====

    @Test
    public void testMpNativeUserTableCreated() throws Exception {
        String tableName = getExistingTableName("mp_user", "t_mp_user");
        assertNotNull(tableName, "MP 原生 @TableName('mp_user') 应生成表，实际表: " + getAllTables());
    }

    @Test
    public void testMpNativeUserColumns() throws Exception {
        // 表名可能是 mp_user 或 t_mp_user，按实际存在的来查
        String tableName = getExistingTableName("MP_USER", "T_MP_USER");
        assertNotNull(tableName, "应找到 mp_user 相关的表");

        Set<String> columns = getTableColumns(tableName);
        assertTrue(columns.contains("ID"), "应包含 ID 列，实际列: " + columns);
        assertTrue(columns.contains("USER_NAME"), "应包含 USER_NAME 列（@TableField('user_name')），实际列: " + columns);
        assertTrue(columns.contains("EMAIL") || columns.contains("email"),
                "应包含 EMAIL 列，实际列: " + columns);
        assertTrue(columns.contains("AGE") || columns.contains("age"),
                "应包含 AGE 列，实际列: " + columns);
        assertFalse(columns.contains("TRANSIENT_FIELD"),
                "@TableField(exist=false) 的字段不应建列，实际列: " + columns);
    }

    // ===== @Ignore 注解验证 =====

    @Test
    public void testIgnoreAnnotationOnMpField() throws Exception {
        // MpNativeUser 的 ignoredField 标注了 @Ignore，不应建列
        String tableName = getExistingTableName("MP_USER", "T_MP_USER");
        assertNotNull(tableName, "mp_user 表应被创建");

        Set<String> columns = getTableColumns(tableName);
        assertFalse(columns.contains("IGNORED_FIELD"),
                "@Ignore 的字段不应建列，实际列: " + columns);
    }

    // ===== 逻辑删除默认值验证 =====

    @Test
    public void testLogicalDeleteDefaultValue() throws Exception {
        // MpNativeUser 有 deleted 字段，mybatis-plus 配置 logic-not-delete-value=0
        String tableName = getExistingTableName("MP_USER", "T_MP_USER");
        assertNotNull(tableName, "mp_user 表应被创建");

        String defaultValue = getColumnDefault(tableName, "DELETED");
        assertNotNull(defaultValue,
                "deleted 列应有默认值（logicNotDeleteValue=0），实际列信息: " + getColumnInfo(tableName));
        assertTrue(defaultValue.contains("0"),
                "deleted 列默认值应包含 '0'，实际: " + defaultValue);
    }

    // ===== 配置桥接验证（从 SqlSessionFactory 读取实际运行时配置）=====

    @Test
    public void testConfigBridging() {
        org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig config =
                context.getBean(org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig.class);

        assertEquals("t_", config.getTablePrefix(), "tablePrefix 应从 SqlSessionFactory 桥接");
        assertTrue(config.isMapUnderscoreToCamelCase(), "mapUnderscoreToCamelCase 应为 true（MP 内部默认值）");
        assertEquals("deleted", config.getLogicDeleteField(), "logicDeleteField 应从 SqlSessionFactory 桥接");
        assertEquals("0", config.getLogicNotDeleteValue(), "logicNotDeleteValue 应从 SqlSessionFactory 桥接");
    }

    /**
     * 核心验证：adapter 配置与 SqlSessionFactory 实际运行时配置一致。
     * <p>
     * application.yml 中未显式配置 map-underscore-to-camel-case，
     * MybatisPlusProperties 中该值可能是 MyBatis 原始默认值 false，
     * 但 MP 内部在构建 SqlSessionFactory 时会将其改为 true。
     * adapter 应读取 SqlSessionFactory 的实际值（true），而非 Properties 的原始值。
     */
    @Test
    public void testConfigMatchesSqlSessionFactory() {
        SqlSessionFactory sqlSessionFactory = context.getBean(SqlSessionFactory.class);
        org.apache.ibatis.session.Configuration mpConfiguration = sqlSessionFactory.getConfiguration();
        GlobalConfig globalConfig = GlobalConfigUtils.getGlobalConfig(mpConfiguration);
        GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();

        org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig adapterConfig =
                context.getBean(org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig.class);

        // 验证 adapter 配置与 SqlSessionFactory 实际配置完全一致
        assertEquals(mpConfiguration.isMapUnderscoreToCamelCase(), adapterConfig.isMapUnderscoreToCamelCase(),
                "adapter 的 mapUnderscoreToCamelCase 应与 SqlSessionFactory 一致");
        assertEquals(dbConfig.getTablePrefix(), adapterConfig.getTablePrefix(),
                "adapter 的 tablePrefix 应与 SqlSessionFactory 一致");
        assertEquals(dbConfig.isCapitalMode(), adapterConfig.isCapitalMode(),
                "adapter 的 capitalMode 应与 SqlSessionFactory 一致");
        assertEquals(dbConfig.getLogicDeleteField(), adapterConfig.getLogicDeleteField(),
                "adapter 的 logicDeleteField 应与 SqlSessionFactory 一致");
        assertEquals(dbConfig.getLogicNotDeleteValue(), adapterConfig.getLogicNotDeleteValue(),
                "adapter 的 logicNotDeleteValue 应与 SqlSessionFactory 一致");
    }

    /**
     * 验证 MP 内部默认值：即使 yml 未配置 map-underscore-to-camel-case，
     * SqlSessionFactory 中该值也应为 true（MP 内部默认行为）。
     */
    @Test
    public void testMpInternalDefaultMapUnderscoreToCamelCase() {
        SqlSessionFactory sqlSessionFactory = context.getBean(SqlSessionFactory.class);
        assertTrue(sqlSessionFactory.getConfiguration().isMapUnderscoreToCamelCase(),
                "MP 内部默认 mapUnderscoreToCamelCase 应为 true（即使 yml 未显式配置）");
    }

    /**
     * 验证 InitializeBeans 机制生效：mpSqlSessionFactoryInitializer Bean 存在。
     */
    @Test
    public void testInitializeBeansRegistered() {
        assertTrue(context.containsBean("mpSqlSessionFactoryInitializer"),
                "mpSqlSessionFactoryInitializer Bean 应被注册（InitializeBeans 机制）");
    }

    // ===== 辅助方法 =====

    private Set<String> getAllTables() throws Exception {
        Set<String> tables = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    private String getExistingTableName(String... candidates) throws Exception {
        Set<String> tables = getAllTables();
        for (String candidate : candidates) {
            // H2 可能存大写或小写，都做比较
            for (String table : tables) {
                if (table.equalsIgnoreCase(candidate)) {
                    return table; // 返回数据库中实际的名称
                }
            }
        }
        return null;
    }

    private Set<String> getTableColumns(String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT UPPER(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = '"
                             + tableName.toUpperCase() + "' AND TABLE_SCHEMA = 'PUBLIC'")) {
            while (rs.next()) {
                columns.add(rs.getString(1));
            }
        }
        return columns;
    }

    private String getColumnDefault(String tableName, String columnName) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = '"
                             + tableName.toUpperCase() + "' AND UPPER(COLUMN_NAME) = '"
                             + columnName.toUpperCase() + "' AND TABLE_SCHEMA = 'PUBLIC'")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return null;
    }

    private String getColumnInfo(String tableName) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COLUMN_NAME, COLUMN_DEFAULT, IS_NULLABLE, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = '"
                             + tableName.toUpperCase() + "' AND TABLE_SCHEMA = 'PUBLIC'")) {
            while (rs.next()) {
                sb.append(String.format("[%s, default=%s, nullable=%s, type=%s] ",
                        rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        }
        return sb.toString();
    }

}
