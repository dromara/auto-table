package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.dromara.autotable.strategy.mysql.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.strategy.mysql.data.MysqlCompareTableInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 logicDropColumnPrefix 功能：逻辑删除字段重命名
 */
public class LogicDropColumnTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
        AutoTableGlobalConfig.clear();
    }

    @Test
    void testMysqlRenameColumnSqlGeneration() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 添加需要重命名的列（逻辑删除）
        compareTableInfo.addRenameColumn("age", "_del_age", "INT");
        compareTableInfo.addRenameColumn("address", "_del_address", "VARCHAR(255)");

        // 生成 SQL
        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        assertFalse(sqlList.isEmpty(), "应该生成 SQL");
        String sql = sqlList.get(0);

        // 验证包含 CHANGE COLUMN 语句
        assertTrue(sql.contains("CHANGE COLUMN"), "应该包含 CHANGE COLUMN");
        assertTrue(sql.contains("`_del_age`"), "应该包含重命名后的列名 _del_age");
        assertTrue(sql.contains("`_del_address`"), "应该包含重命名后的列名 _del_address");
        assertTrue(sql.contains("`age`"), "应该包含原列名 age");
        assertTrue(sql.contains("`address`"), "应该包含原列名 address");
    }

    @Test
    void testMysqlCompareTableInfoNeedModifyWithRenameColumn() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 初始状态，没有重命名列
        assertFalse(compareTableInfo.needModify(), "初始状态不应该需要修改");

        // 添加重命名列
        compareTableInfo.addRenameColumn("age", "_del_age", "INT");

        // 验证需要修改
        assertTrue(compareTableInfo.needModify(), "添加重命名列后应该需要修改");
    }

    @Test
    void testMysqlValidateFailedMessageWithRenameColumn() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");
        compareTableInfo.addRenameColumn("age", "_del_age", "INT");

        String message = compareTableInfo.validateFailedMessage();

        assertTrue(message.contains("重命名列（逻辑删除）"), "应该包含重命名列的提示信息");
        assertTrue(message.contains("age -> _del_age"), "应该包含具体的重命名映射");
    }

    @Test
    void testMysqlSkipAlreadyPrefixedColumn() {
        // 验证：已带前缀的字段不应该被再次处理
        // 这个测试验证比较逻辑，需要在实际策略中验证
        // 这里仅测试 CompareTableInfo 的行为

        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");
        compareTableInfo.addRenameColumn("_del_age", "_del__del_age", "INT");

        assertTrue(compareTableInfo.needModify(), "即使重复前缀也应该允许重命名（由策略层控制跳过逻辑）");
    }

    @Test
    void testMysqlEmptyRenameColumnMap() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 空的重命名列列表
        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        // 不应该生成任何 SQL（因为没有需要修改的内容）
        assertTrue(sqlList.isEmpty(), "空的重命名列不应该生成 SQL");
    }
}
