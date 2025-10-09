package org.dromara.autotable.test.core.entity.mysql_update;

import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.TableIndexes;

@FieldNameConstants
@TableIndexes({
        @TableIndex(fields = {TestIndex.Fields.testColumn3, TestIndex.Fields.testColumn2}, comment = "测试索引3,2"),
})
@AutoTable
public class TestIndex {
    private String testColumn;
    private String testColumn2;
    private String testColumn3;
    private String testColumn4;
}
