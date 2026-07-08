package org.dromara.autotable.strategy.mysql;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlCompareTableInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MysqlModifyTableSqlBuilder 单元测试。
 *
 * <p>策略模块自带单元测试，不依赖 auto-table-test-core（避免集成测试模块的编译依赖），
 * 与 {@code SqlServerModifyTableSqlBuilderTest} 对称。</p>
 */
public class MysqlModifyTableSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    /**
     * 回归测试：schema 未配置时为 null（getTableSchema 默认实现返回 null），
     * 构造 MysqlCompareTableInfo 不应抛 NPE（issue #11，v2.6.0 回归）。
     * MySQL 的 ALTER TABLE 不使用 schema 前缀，故 null 不影响 SQL 生成。
     */
    @Test
    void test_schema为null时不抛NPE且SQL不带schema前缀() {
        // 2.5.17 schema 为 ""，2.6.0 重构后变为 null，曾因构造函数 @NonNull 抛 NPE 致启动失败
        MysqlCompareTableInfo compareTableInfo = assertDoesNotThrow(() -> new MysqlCompareTableInfo("test_table", null));

        MysqlColumnMetadata columnMetadata = new MysqlColumnMetadata();
        columnMetadata.setName("event_id");
        columnMetadata.setType(new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()));
        columnMetadata.setNotNull(true);
        columnMetadata.setPosition(1);
        compareTableInfo.addEditColumnMetadata(columnMetadata);

        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        assertFalse(sqlList.isEmpty(), "应该生成 SQL");
        // MySQL 不读取 schema，表名直接包裹为 `test_table`，不含 schema 前缀
        assertTrue(sqlList.get(0).contains("ALTER TABLE `test_table`"), "不应包含 schema 前缀");
    }
}
