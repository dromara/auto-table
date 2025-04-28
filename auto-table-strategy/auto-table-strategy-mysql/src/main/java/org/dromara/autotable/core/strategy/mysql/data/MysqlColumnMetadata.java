package org.dromara.autotable.core.strategy.mysql.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.strategy.ColumnMetadata;

/**
 * mysql有部分特殊注解，继承ColumnMetadata，拓展额外信息
 *
 * @author don
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
@Accessors(chain = true)
public class MysqlColumnMetadata extends ColumnMetadata {

    /**
     * 无符号：数字类型不允许负数，其范围从 0 开始
     */
    private boolean unsigned;

    /**
     * 零填充: 数字类型，位数不足的前面补0
     */
    private boolean zerofill;

    /**
     * 默认字符集
     */
    private String characterSet;

    /**
     * 默认排序规则
     */
    private String collate;

    /**
     * 当前字段的顺序位置，按照实体字段自上而下排列的，父类的字段整体排在子类之后
     */
    private int position;

    /**
     * <p>表示前一列的列名，该值的使用规则如下:
     * <p>if 非空，生成“AFTER [newPreColumn]”，表示位于某列之后；
     * <p>else if 空字符，生成“FIRST”，表示第一列；
     * <p>else 生成空字符串，表示没有变动；
     */
    private String newPreColumn;

    public boolean hasQualifier() {
        return unsigned || zerofill;
    }
}
