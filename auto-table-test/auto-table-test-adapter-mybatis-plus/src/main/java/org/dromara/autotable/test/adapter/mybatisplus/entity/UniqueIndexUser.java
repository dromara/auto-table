package org.dromara.autotable.test.adapter.mybatisplus.entity;

import lombok.Data;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.Column;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.ColumnId;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.Table;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.UniqueIndex;
import org.dromara.autotable.annotation.AutoTable;

/**
 * 验证 @UniqueIndex 唯一索引注解
 */
@Data
@AutoTable
@Table(value = "unique_index_user", comment = "唯一索引测试用户表")
public class UniqueIndexUser {

    @ColumnId(comment = "主键ID")
    private Long id;

    @UniqueIndex(name = "uk_email", comment = "邮箱唯一索引")
    @Column(value = "email", comment = "邮箱", notNull = true)
    private String email;

    @Column(value = "phone", comment = "手机号")
    private String phone;
}
