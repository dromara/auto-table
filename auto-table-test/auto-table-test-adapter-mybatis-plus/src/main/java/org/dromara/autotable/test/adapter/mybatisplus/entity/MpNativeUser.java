package org.dromara.autotable.test.adapter.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.dromara.autotable.annotation.AutoTable;

/**
 * MP 原生注解实体，验证 adapter 基础层（不依赖自定义注解）
 */
@Data
@AutoTable
@TableName("mp_user")
public class MpNativeUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_name")
    private String userName;

    private String email;

    private Integer age;

    @TableField(exist = false)
    private String transientField;

    private Integer deleted;
}
