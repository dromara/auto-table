package org.dromara.autotable.strategy.oracle;

import lombok.Getter;
import lombok.Setter;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IndexMetadata;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * oracle变更信息
 */
@Getter
@Setter
public class OracleCompareTableInfo extends CompareTableInfo {
    /*private DefaultTableMetadata tableMetadata;
    private TabComment tabComment;
    private Map<String, TabColumn> tabColumnMap;
    private Map<String, List<TabIndex>> tabIndexMap;*/

    /**
     * 是否需要自增主键序列
     */
    private boolean needSequence;
    /**
     * 是否存在自增主键序列
     */
    private boolean hasSequence;

    /**
     * 更新表注释
     */
    private String tableComment;
    /**
     * 删除主键
     */
    private TabColumn deletePrimaryKey;
    /**
     * 新增主键
     */
    private ColumnMetadata createPrimaryKey;


    /**
     * 新增字段
     */
    private List<ColumnMetadata> createColumnList;
    /**
     * 删除字段
     */
    private List<String> deleteColumnList;
    /**
     * 更新字段
     */
    private List<String> updateColumnList;
    /**
     * 更新字段注释
     */
    private List<ColumnMetadata> updateColumnCommentList;

    /**
     * 删除索引
     */
    private Set<String> deleteIndexList;
    /**
     * 新增索引
     */
    private List<IndexMetadata> createIndexList;

    public OracleCompareTableInfo(String name, String schema) {
        super(name, schema);
    }

    public boolean needModify() {
        return tableComment != null
                || deletePrimaryKey != null
                || createPrimaryKey != null
                || !createColumnList.isEmpty()
                || !deleteColumnList.isEmpty()
                || !updateColumnList.isEmpty()
                || !updateColumnCommentList.isEmpty()
                || !createIndexList.isEmpty()
                || !deleteIndexList.isEmpty()
                ;
    }

    public String validateFailedMessage() {
        StringBuilder failedMessage = new StringBuilder();
        if (tableComment != null) {
            failedMessage.append("表注释变更：").append(tableComment).append("\n");
        }
        if (deletePrimaryKey != null) {
            failedMessage.append("删除主键：").append(deletePrimaryKey.getColumn_name()).append("\n");
        }
        if (createPrimaryKey != null) {
            failedMessage.append("新增主键：").append(createPrimaryKey.getName()).append("\n");
        }
        if (!createColumnList.isEmpty()) {
            failedMessage.append("新增字段：").append(
                    createColumnList.stream().map(ColumnMetadata::getName).collect(Collectors.joining(", "))
            ).append("\n");
        }
        if (!deleteColumnList.isEmpty()) {
            failedMessage.append("删除字段：").append(
                    String.join(", ", deleteColumnList)
            ).append("\n");
        }
        for (String updateColumn : updateColumnList) {
            failedMessage.append("修改字段：").append(updateColumn).append("\n");
        }
        if (!updateColumnCommentList.isEmpty()) {
            failedMessage.append("新增字段注释：").append(
                    updateColumnCommentList.stream().map(ColumnMetadata::getName).collect(Collectors.joining(", "))
            ).append("\n");
        }

        if (!deleteIndexList.isEmpty()) {
            failedMessage.append("删除索引：").append(
                    String.join(", ", deleteIndexList)
            ).append("\n");
        }
        if (!createIndexList.isEmpty()) {
            failedMessage.append("新增索引：").append(
                    createIndexList.stream().map(IndexMetadata::getName).collect(Collectors.joining(", "))
            ).append("\n");
        }
        return failedMessage.toString();
    }
}
