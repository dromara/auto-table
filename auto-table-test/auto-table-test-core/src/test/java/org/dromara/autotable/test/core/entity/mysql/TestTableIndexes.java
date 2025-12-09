package org.dromara.autotable.test.core.entity.mysql;

import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.TableIndexes;

@FieldNameConstants
@TableIndexes({
        @TableIndex(fields = {TestTableIndexes.Fields.testColumn}, comment = "测试索引1"),
        @TableIndex(fields = {TestTableIndexes.Fields.testColumn2}, comment = "测试索引2"),
})
@AutoTable
public class TestTableIndexes {
    private String testColumn;
    private String testColumn2;
    private String testColumn3;
    private String testColumn4;
}
