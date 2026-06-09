package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.strategy.mysql.ColumnPositionHelper;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.dbdata.InformationSchemaColumn;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ColumnPositionHelper 单元测试
 */
public class ColumnPositionHelperTest {

    private InformationSchemaColumn dbCol(int position, String name) {
        InformationSchemaColumn col = new InformationSchemaColumn();
        col.setColumnName(name);
        col.setOrdinalPosition(position);
        return col;
    }

    private MysqlColumnMetadata expectCol(int position, String name) {
        MysqlColumnMetadata col = new MysqlColumnMetadata();
        col.setName(name);
        col.setPosition(position);
        return col;
    }

    @Test
    void testNoChange() {
        // 数据库和期望顺序完全一致
        List<InformationSchemaColumn> dbColumns = Arrays.asList(
            dbCol(1, "A"), dbCol(2, "B"), dbCol(3, "C")
        );
        List<MysqlColumnMetadata> expectPositions = Arrays.asList(
            expectCol(1, "A"), expectCol(2, "B"), expectCol(3, "C")
        );

        ColumnPositionHelper.generateChangePosition(dbColumns, expectPositions);

        // 没有任何变化，newPreColumn 应该都是 null
        for (MysqlColumnMetadata col : expectPositions) {
            assertNull(col.getNewPreColumn(), "无变化时 newPreColumn 应该为 null");
        }
    }

    @Test
    void testNewColumnsAdded() {
        // 数据库只有 A B，期望多了 C D
        List<InformationSchemaColumn> dbColumns = Arrays.asList(
            dbCol(1, "A"), dbCol(2, "B")
        );
        List<MysqlColumnMetadata> expectPositions = Arrays.asList(
            expectCol(1, "A"), expectCol(2, "B"), expectCol(3, "C"), expectCol(4, "D")
        );

        ColumnPositionHelper.generateChangePosition(dbColumns, expectPositions);

        // A B 无变化
        assertNull(expectPositions.get(0).getNewPreColumn());
        assertNull(expectPositions.get(1).getNewPreColumn());
        // C D 是新增的，不应该有位置变更（由 ADD COLUMN 处理）
        assertNull(expectPositions.get(2).getNewPreColumn());
        assertNull(expectPositions.get(3).getNewPreColumn());
    }

    @Test
    void testColumnsDeleted() {
        // 数据库有 A B C D，期望只有 A C（删除了 B D）
        // 删除后剩余列 A C 的相对顺序不变，无需移动
        List<InformationSchemaColumn> dbColumns = Arrays.asList(
            dbCol(1, "A"), dbCol(2, "B"), dbCol(3, "C"), dbCol(4, "D")
        );
        List<MysqlColumnMetadata> expectPositions = Arrays.asList(
            expectCol(1, "A"), expectCol(2, "C")
        );

        ColumnPositionHelper.generateChangePosition(dbColumns, expectPositions);

        // A 和 C 的相对顺序不变，无需移动
        assertNull(expectPositions.get(0).getNewPreColumn(), "A 无需移动");
        assertNull(expectPositions.get(1).getNewPreColumn(), "C 无需移动（相对顺序不变）");
    }

    @Test
    void testFullReorder() {
        // 数据库是 A B C，期望是 C A B
        List<InformationSchemaColumn> dbColumns = Arrays.asList(
            dbCol(1, "A"), dbCol(2, "B"), dbCol(3, "C")
        );
        List<MysqlColumnMetadata> expectPositions = Arrays.asList(
            expectCol(1, "C"), expectCol(2, "A"), expectCol(3, "B")
        );

        ColumnPositionHelper.generateChangePosition(dbColumns, expectPositions);

        // C 应该移到最前面（newPreColumn 为空字符串表示 FIRST）
        assertNotNull(expectPositions.get(0).getNewPreColumn(), "C 应该有位置变化");
        assertEquals("", expectPositions.get(0).getNewPreColumn());
    }

    @Test
    void testPartialReorder() {
        // 数据库是 A B C D E，期望是 A C B D E（只交换了 B C）
        List<InformationSchemaColumn> dbColumns = Arrays.asList(
            dbCol(1, "A"), dbCol(2, "B"), dbCol(3, "C"), dbCol(4, "D"), dbCol(5, "E")
        );
        List<MysqlColumnMetadata> expectPositions = Arrays.asList(
            expectCol(1, "A"), expectCol(2, "C"), expectCol(3, "B"), expectCol(4, "D"), expectCol(5, "E")
        );

        ColumnPositionHelper.generateChangePosition(dbColumns, expectPositions);

        // A 无变化
        assertNull(expectPositions.get(0).getNewPreColumn());
        // C 应该有位置变化（移到 A 后面）
        assertNotNull(expectPositions.get(1).getNewPreColumn(), "C 应该有位置变化");
        // D E 无变化
        assertNull(expectPositions.get(3).getNewPreColumn());
        assertNull(expectPositions.get(4).getNewPreColumn());
    }

    @Test
    void testSwapTwoColumns() {
        // 数据库是 A B，期望是 B A（交换两个列）
        List<InformationSchemaColumn> dbColumns = Arrays.asList(
            dbCol(1, "A"), dbCol(2, "B")
        );
        List<MysqlColumnMetadata> expectPositions = Arrays.asList(
            expectCol(1, "B"), expectCol(2, "A")
        );

        ColumnPositionHelper.generateChangePosition(dbColumns, expectPositions);

        // B 应该移到最前面
        assertEquals("", expectPositions.get(0).getNewPreColumn());
    }

    @Test
    void testEmptyDatabase() {
        // 数据库为空，期望有 A B C（全是新增）
        List<InformationSchemaColumn> dbColumns = new ArrayList<>();
        List<MysqlColumnMetadata> expectPositions = Arrays.asList(
            expectCol(1, "A"), expectCol(2, "B"), expectCol(3, "C")
        );

        ColumnPositionHelper.generateChangePosition(dbColumns, expectPositions);

        // 全是新增，不应该有位置变更
        for (MysqlColumnMetadata col : expectPositions) {
            assertNull(col.getNewPreColumn());
        }
    }

    @Test
    void testEmptyExpectation() {
        // 数据库有 A B C，期望为空（全删除）
        List<InformationSchemaColumn> dbColumns = Arrays.asList(
            dbCol(1, "A"), dbCol(2, "B"), dbCol(3, "C")
        );
        List<MysqlColumnMetadata> expectPositions = new ArrayList<>();

        ColumnPositionHelper.generateChangePosition(dbColumns, expectPositions);

        // 期望为空，不应该有任何操作
        assertTrue(expectPositions.isEmpty());
    }
}
