package org.dromara.autotable.core.strategy.oracle;

import lombok.Data;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 字段信息
 */
@Data
public class TabColumn {


    private String table_name;

    private String column_name;

    private String data_type;

    private Integer data_length;

    private Integer data_precision;

    private Integer data_scale;

    private String nullable;

    private Integer column_id;

    private String data_default;

    private String data_default_vc;

    private String comments;

    private String constraint_name;

    private String constraint_type;

    public String getFullType() {
        String fullType = data_type;
        if (fullType.toLowerCase().contains("clob")) {
            return fullType;
        }
        if (fullType.contains("(") && fullType.contains(")")) {
            return fullType;
        }
        if (data_length == null && data_precision == null && data_scale == null) {
            return fullType;
        }
        if (data_precision == null && data_scale == null) {
            return fullType + "(" + data_length + ")";
        }
        if (data_precision != null) {
            fullType += "(" + data_precision;
            if (data_scale != null) {
                fullType += "," + data_scale;
            }
            fullType += ")";
        }
        return fullType;
    }

    public boolean hasChangeType(ColumnMetadata newColumn) {
        String newType = newColumn.getType().getDefaultFullType();
        String oldType = this.getFullType();
        return !newType.equals(oldType);
    }

    public boolean hasChangeNull(ColumnMetadata newColumn) {
        boolean newNullAble = !newColumn.isNotNull();
        boolean oldNullAble = "Y".equals(this.nullable);
        return newNullAble == oldNullAble;
    }


    public boolean hasChangeDefaultValue(ColumnMetadata newColumn) {
        String newValue = Optional.ofNullable(newColumn.getDefaultValue()).orElse("");
        String oldValue = Optional.ofNullable(this.data_default_vc).orElse("");
        if (DefaultValueEnum.NULL.equals(newColumn.getDefaultValueType())) {
            return StringUtils.hasText(oldValue);
        }
        if (DefaultValueEnum.EMPTY_STRING.equals(newColumn.getDefaultValueType())) {
            return !"''".equals(oldValue);
        }
        if (oldValue.startsWith("'")) {
            oldValue = oldValue.substring(1, oldValue.length() - 1);
            return !newValue.equals(oldValue);
        }
        return !newValue.equalsIgnoreCase(oldValue);
    }

    public boolean hasChangeComment(ColumnMetadata newColumn) {
        String newComment = Optional.ofNullable(newColumn.getComment()).orElse("");
        String oldComment = Optional.ofNullable(this.comments).orElse("");
        return !newComment.equals(oldComment);
    }


    public static List<TabColumn> search(String tableName) {
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);
        String sql = "SELECT tc.table_name\n" +
                "     , tc.column_name\n" +
                "     , tc.data_type\n" +
                "     , tc.data_length\n" +
                "     , tc.data_precision\n" +
                "     , tc.data_scale\n" +
                "     , tc.nullable\n" +
                "     , tc.column_id\n" +
                "     , tc.data_default\n" +
                "     , tc.data_default_vc\n" +
                "     , cc.comments\n" +
                "     , pk.constraint_name\n" +
                "     , pk.constraint_type\n" +
                "      FROM user_tab_columns tc\n" +
                "               LEFT JOIN user_col_comments cc ON tc.table_name = cc.table_name AND tc.column_name = cc.column_name\n" +
                "               LEFT JOIN (SELECT cons_col.table_name\n" +
                "                               , cons_col.column_name\n" +
                "                               , cons_col.constraint_name\n" +
                "                               , cons.constraint_type\n" +
                "                          FROM user_cons_columns cons_col\n" +
                "                                   LEFT JOIN user_constraints cons ON cons_col.constraint_name = cons.constraint_name\n" +
                "                          WHERE cons.constraint_type = 'P') pk\n" +
                "                         ON tc.table_name = pk.table_name AND tc.column_name = pk.column_name\n" +
                "      WHERE UPPER(tc.table_name) = UPPER(':tableName')\n" +
                "      ORDER BY tc.column_id";
        return OracleHelper.DB.queryList(sql, params, TabColumn.class);
    }


}
