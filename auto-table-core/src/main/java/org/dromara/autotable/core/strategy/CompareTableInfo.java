package org.dromara.autotable.core.strategy;

import lombok.Getter;
import lombok.NonNull;

/**
 * 比对表与实体的数据模型接口
 *
 * @author don
 */
@Getter
public abstract class CompareTableInfo {
    /**
     * 表名: 不可变，变了意味着新表
     */
    @NonNull
    protected final String name;

    /**
     * schema
     */
    protected String schema;

    public CompareTableInfo(@NonNull String name, String schema) {
        this.name = name;
        this.schema = schema;
    }

    /**
     * 是否需要修改表,即表与模型是否存在差异
     * @return 是否需要修改表
     */
    public abstract boolean needModify();

    /**
     * 验证模式下失败的信息
     * @return 验证模式下失败的信息
     */
    public abstract String validateFailedMessage();
}
