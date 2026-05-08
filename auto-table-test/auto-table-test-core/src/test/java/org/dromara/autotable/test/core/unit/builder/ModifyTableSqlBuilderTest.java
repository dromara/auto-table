package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
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
 * 测试 ModifyTableSqlBuilder 生成的主键字段修改 SQL
 */
public class ModifyTableSqlBuilderTest {

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
    void testModifyPrimaryKeyColumn() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 构造一个需要修改的主键字段
        MysqlColumnMetadata columnMetadata = new MysqlColumnMetadata();
        columnMetadata.setName("event_id");
        columnMetadata.setComment("事件id");
        columnMetadata.setType(new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()));
        columnMetadata.setNotNull(true);
        columnMetadata.setPrimary(true);
        columnMetadata.setPosition(1);

        compareTableInfo.addEditColumnMetadata(columnMetadata);

        // 生成 SQL
        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        System.out.println("Generated SQL:");
        for (String sql : sqlList) {
            System.out.println(sql);
        }

        assertFalse(sqlList.isEmpty(), "应该生成 SQL");
        String sql = sqlList.get(0);
        assertTrue(sql.contains("MODIFY COLUMN"), "应该包含 MODIFY COLUMN");
        assertTrue(sql.contains("event_id"), "应该包含 event_id");
        assertTrue(sql.contains("varchar(128)"), "应该包含 varchar(128)");
    }
}
