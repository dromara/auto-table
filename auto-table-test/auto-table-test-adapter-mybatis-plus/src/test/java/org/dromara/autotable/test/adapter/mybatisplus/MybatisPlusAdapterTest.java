package org.dromara.autotable.test.adapter.mybatisplus;

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
 * 验证 auto-table-adapter-mybatis-plus-spring-boot-starter 在 Spring Boot 环境下的完整集成。
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
        assertTrue(context.containsBean("mybatisPlusExtendedMetadataAdapter"),
                "mybatisPlusExtendedMetadataAdapter Bean 应被自动注册");
        assertTrue(context.containsBean("mybatisPlusExtendedClassScanner"),
                "mybatisPlusExtendedClassScanner Bean 应被自动注册");
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

    // ===== 自定义注解建表验证 =====

    @Test
    public void testCustomOrderTableCreated() throws Exception {
        String tableName = getExistingTableName("t_custom_order", "custom_order");
        assertNotNull(tableName, "自定义 @Table('custom_order') 应生成表，实际表: " + getAllTables());
    }

    @Test
    public void testCustomOrderColumns() throws Exception {
        String tableName = getExistingTableName("T_CUSTOM_ORDER", "CUSTOM_ORDER");
        assertNotNull(tableName, "应找到 custom_order 相关的表");

        Set<String> columns = getTableColumns(tableName);
        assertTrue(columns.contains("ID"), "应包含 ID 列（@ColumnId），实际列: " + columns);
        assertTrue(columns.contains("ORDER_NO"), "应包含 ORDER_NO 列（@Column('order_no')），实际列: " + columns);
        assertTrue(columns.contains("AMOUNT") || columns.contains("amount"),
                "应包含 AMOUNT 列，实际列: " + columns);
        assertTrue(columns.contains("STATUS") || columns.contains("status"),
                "应包含 STATUS 列，实际列: " + columns);
        assertFalse(columns.contains("TEMP_DATA"),
                "@TableField(exist=false) 的字段不应建列，实际列: " + columns);
    }

    // ===== 表注释验证 =====

    @Test
    public void testTableComment() throws Exception {
        // @Table(comment = "订单表") 通过 @AliasFor 合并到 @AutoTable.comment，
        // 再由 CustomAnnotationFinder（Spring Boot 环境下）正确读取
        String tableName = getExistingTableName("CUSTOM_ORDER", "T_CUSTOM_ORDER");
        assertNotNull(tableName, "custom_order 表应被创建");

        String comment = getTableComment(tableName);
        // H2 2.x 中 COMMENT ON TABLE 后 REMARKS 可通过 INFORMATION_SCHEMA.TABLES 查询
        assertNotNull(comment, "表注释不应为 null，@Table(comment) 应正确传播。"
                + " 表名=" + tableName + ", 所有表=" + getAllTablesWithRemarks());
        assertTrue(comment.contains("订单表"), "表注释应包含 '订单表'，实际: " + comment);
    }

    // ===== 唯一索引验证 =====

    @Test
    public void testUniqueIndex() throws Exception {
        String tableName = getExistingTableName("unique_index_user", "t_unique_index_user");
        assertNotNull(tableName, "UniqueIndexUser 表应被创建");

        // 查询该表的唯一索引
        Set<String> uniqueIndexes = getUniqueIndexNames(tableName);
        assertFalse(uniqueIndexes.isEmpty(),
                "应有唯一索引，实际索引: " + getAllIndexNames(tableName));
        // 索引名可能是 UK_EMAIL 或包含 email 列的唯一索引
        boolean hasEmailUniqueIndex = uniqueIndexes.stream()
                .anyMatch(name -> name.toUpperCase().contains("UK_EMAIL")
                        || name.toUpperCase().contains("EMAIL"));
        assertTrue(hasEmailUniqueIndex,
                "应有 email 列的唯一索引，实际: " + uniqueIndexes);
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

    // ===== 配置桥接验证 =====

    @Test
    public void testConfigBridging() {
        org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig config =
                context.getBean(org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig.class);

        assertEquals("t_", config.getTablePrefix(), "tablePrefix 应从 MP Properties 桥接");
        assertTrue(config.isMapUnderscoreToCamelCase(), "mapUnderscoreToCamelCase 应为 true");
        assertEquals("deleted", config.getLogicDeleteField(), "logicDeleteField 应从 MP Properties 桥接");
        assertEquals("0", config.getLogicNotDeleteValue(), "logicNotDeleteValue 应从 MP Properties 桥接");
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

    private String getTableComment(String tableName) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT REMARKS FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = '"
                             + tableName.toUpperCase() + "' AND TABLE_SCHEMA = 'PUBLIC'")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return null;
    }

    private Set<String> getUniqueIndexNames(String tableName) throws Exception {
        Set<String> indexes = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(TABLE_NAME) = '"
                             + tableName.toUpperCase() + "' AND INDEX_TYPE_NAME = 'UNIQUE INDEX'")) {
            while (rs.next()) {
                indexes.add(rs.getString(1));
            }
        }
        return indexes;
    }

    private Set<String> getAllIndexNames(String tableName) throws Exception {
        Set<String> indexes = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE UPPER(TABLE_NAME) = '"
                             + tableName.toUpperCase() + "'")) {
            while (rs.next()) {
                indexes.add(rs.getString(1));
            }
        }
        return indexes;
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

    private String getAllTablesWithRemarks() throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT TABLE_NAME, REMARKS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")) {
            while (rs.next()) {
                sb.append(String.format("[%s, remarks=%s] ", rs.getString(1), rs.getString(2)));
            }
        }
        return sb.toString();
    }
}
