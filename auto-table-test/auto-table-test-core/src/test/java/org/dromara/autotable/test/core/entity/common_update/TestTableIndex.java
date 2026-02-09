package org.dromara.autotable.test.core.entity.common_update;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.TableIndexes;

@Data
@FieldNameConstants
@AutoTable
@TableIndexes({
        // 这里建议起上名字，因为当你添加fields时候，AutoTable是根据名字索引修改（删除&新增）的，此时如果你配置了 auto-table.auto-drop-index = false, 则会导致新老索引共存
        // @TableIndex(name = "biz1", fields = {TestTableIndexes.Fields.tableIndex1, TestTableIndexes.Fields.tableIndex2}, comment = "这是一个通过@TableIndex创建的索引")
        // 因为指定了name，所以会覆盖原来的索引
        @TableIndex(name = "biz1", fields = {TestTableIndex.Fields.tableIndex1, TestTableIndex.Fields.tableIndex2, TestTableIndex.Fields.tableIndex3}, comment = "这是一个通过@TableIndex创建的索引")
})
public class TestTableIndex {
    private String tableIndex1;
    private String tableIndex2;
    private String tableIndex3;
}
