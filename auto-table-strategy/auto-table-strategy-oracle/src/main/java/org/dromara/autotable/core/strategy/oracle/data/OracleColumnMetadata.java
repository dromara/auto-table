package org.dromara.autotable.core.strategy.oracle.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaColumn;

/**
 * Oracle有部分特殊注解，继承ColumnMetadata，拓展额外信息
 * @author KThirty
 * @since 2025/5/22 14:21
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class OracleColumnMetadata extends ColumnMetadata {

    /**
     * 当前字段的顺序位置，按照实体字段自上而下排列的，父类的字段整体排在子类之后
     */
    private int position;
    private Class<?> fieldType;
    /**
     * <p>表示前一列的列名，该值的使用规则如下:
     * <p>if 非空，生成“AFTER [newPreColumn]”，表示位于某列之后；
     * <p>else if 空字符，生成“FIRST”，表示第一列；
     * <p>else 生成空字符串，表示没有变动；
     */
    private String newPreColumn;
    /**
     * 数据库中定义的原始信息
     */
    private InformationSchemaColumn originalColumn;



    public Integer getLength(){
        return super.getType() != null ? super.getType().getLength() : null;
    }
    public Integer getScale(){
        return super.getType() != null ? super.getType().getDecimalLength() : null;
    }


}
