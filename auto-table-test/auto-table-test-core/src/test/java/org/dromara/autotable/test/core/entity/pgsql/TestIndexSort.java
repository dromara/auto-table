package org.dromara.autotable.test.core.entity.pgsql;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.IndexField;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;

@Data
@FieldNameConstants
@AutoTable
@TableIndex(indexFields = {
        @IndexField(field = TestIndexSort.Fields.testColumn1, sort = IndexSortTypeEnum.ASC),
        @IndexField(field = TestIndexSort.Fields.testColumn2, sort = IndexSortTypeEnum.DESC)
})
public class TestIndexSort {
    private String testColumn1;
    private String testColumn2;
}
