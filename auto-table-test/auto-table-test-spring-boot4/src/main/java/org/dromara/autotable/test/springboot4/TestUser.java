package org.dromara.autotable.test.springboot4;

import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.PrimaryKey;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试实体类 - 用户
 */
@Data
@AutoTable(value = "test_user")
public class TestUser implements Serializable {

    @PrimaryKey
    @AutoColumn(comment = "主键ID", sort = 0)
    private Long id;

    @AutoColumn(comment = "用户名", sort = 1, length = 50)
    private String username;

    @AutoColumn(comment = "邮箱", sort = 2, length = 100)
    private String email;

    @AutoColumn(comment = "年龄", sort = 3)
    private Integer age;

    @AutoColumn(comment = "状态", sort = 4)
    private Integer status;

    @AutoColumn(comment = "创建时间", sort = 5)
    private LocalDateTime createTime;

    @AutoColumn(comment = "更新时间", sort = 6)
    private LocalDateTime updateTime;
}
