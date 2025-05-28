package org.dromara.autotable.test.core.entity.common;

import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.pgsql.PgsqlTypeConstant;

@Data
@AutoTable(comment = "包含特殊字符','¥#(|)\"")
public class TestCommentSpecialCharacter {
    // 测试备注中的单引号'bug
    @AutoColumn(comment = "包含特殊字符','¥#(|)\"")
    private String testColumn;
}
