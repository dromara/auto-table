package org.dromara.autotable.core.strategy.oracle.data;

import lombok.*;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IndexMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Oracle表结构对比信息
 * 模仿MysqlCompareTableInfo实现
 */
@Getter
@Setter
public class OracleCompareTableInfo extends CompareTableInfo {
    /**
     * 新的主键
     */
    private List<OracleColumnMetadata> newPrimaries = new ArrayList<>();
    /**
     * 是否删除主键
     */
    private boolean dropPrimary;
    /**
     * 删除的列：谨慎，会导致数据丢失
     */
    private final List<String> dropColumnList = new ArrayList<>();
    /**
     * 修改的列，包含新增、修改
     */
    private final List<OracleModifyColumnMetadata> modifyOracleColumnMetadataList = new ArrayList<>();
    /**
     * 删除的索引
     */
    private final List<String> dropIndexList = new ArrayList<>();
    /**
     * 索引
     */
    private final List<IndexMetadata> indexMetadataList = new ArrayList<>();
    /**
     * 备注修改
     */
    private final List<OracleComment> commentList = new ArrayList<>();

    public OracleCompareTableInfo(@NonNull String name, @NonNull String schema) {
        super(name, schema);
    }

    /**
     * 判断该修改参数，是不是可用，如果除了name，其他值均没有设置过，则无效，反之有效
     */
    @Override
    public boolean needModify() {
        return !commentList.isEmpty() ||
                dropPrimary ||
                !newPrimaries.isEmpty() ||
                !dropColumnList.isEmpty() ||
                !modifyOracleColumnMetadataList.isEmpty() ||
                !dropIndexList.isEmpty() ||
                !indexMetadataList.isEmpty();
    }

    @Override
    public String validateFailedMessage() {
        StringBuilder errorMsg = new StringBuilder();
        if (dropPrimary) {
            errorMsg.append("删除全部主键").append("\n");
        }
        if (!newPrimaries.isEmpty()) {
            errorMsg.append("新增主键：").append(newPrimaries.stream().map(OracleColumnMetadata::getName).collect(Collectors.joining(","))).append("\n");
        }
        if (!dropColumnList.isEmpty()) {
            errorMsg.append("删除列：").append(String.join(",", dropColumnList)).append("\n");
        }
        if (!commentList.isEmpty()) {
            errorMsg.append("修改注释：").append(commentList.stream().map(it -> it.type + " " + it.name + " " + it.comment).collect(Collectors.joining(","))).append("\n");
        }
        if (!modifyOracleColumnMetadataList.isEmpty()) {
            String addColumn = modifyOracleColumnMetadataList.stream()
                    .filter(m -> m.getType() == ModifyType.ADD)
                    .map(OracleModifyColumnMetadata::getOracleColumnMetadata)
                    .map(OracleColumnMetadata::getName)
                    .collect(Collectors.joining(","));
            if (!addColumn.isEmpty()) {
                errorMsg.append("新增列：").append(addColumn).append("\n");
            }
            String modifyColumn = modifyOracleColumnMetadataList.stream()
                    .filter(m -> m.getType() == ModifyType.MODIFY)
                    .map(OracleModifyColumnMetadata::getOracleColumnMetadata)
                    .map(OracleColumnMetadata::getName)
                    .collect(Collectors.joining(","));
            if (!modifyColumn.isEmpty()) {
                errorMsg.append("修改列：").append(modifyColumn).append("\n");
            }
        }
        if (!dropIndexList.isEmpty()) {
            errorMsg.append("删除索引：").append(String.join(",", dropIndexList)).append("\n");
        }
        if (!indexMetadataList.isEmpty()) {
            errorMsg.append("新增索引：").append(indexMetadataList.stream().map(IndexMetadata::getName).collect(Collectors.joining(","))).append("\n");
        }
        return errorMsg.toString();
    }

    public void addNewColumnMetadata(OracleColumnMetadata oracleColumnMetadata) {
        this.modifyOracleColumnMetadataList.add(new OracleModifyColumnMetadata(ModifyType.ADD, oracleColumnMetadata));
    }

    public void addEditColumnMetadata(OracleColumnMetadata oracleColumnMetadata) {
        this.modifyOracleColumnMetadataList.add(new OracleModifyColumnMetadata(ModifyType.MODIFY, oracleColumnMetadata));
    }
    public void addColumnComment(OracleTableMetadata tableMetadata,OracleColumnMetadata oracleColumnMetadata) {
        this.commentList.add(new OracleComment(CommentType.COLUMN,tableMetadata.getTableName() + "." + oracleColumnMetadata.getName(), oracleColumnMetadata.getComment()));
    }
    public void addTableComment(OracleTableMetadata tableMetadata) {
        this.commentList.add(new OracleComment(CommentType.TABLE,tableMetadata.getTableName(),tableMetadata.getComment()));

    }


        /**
         * 重设主键
         */
    public void resetPrimary(List<OracleColumnMetadata> primaries) {
        this.newPrimaries = primaries;
        this.dropPrimary = true;
    }


    @Data
    @AllArgsConstructor
    public static class OracleModifyColumnMetadata {
        private ModifyType type;
        private OracleColumnMetadata oracleColumnMetadata;
    }

    @Data
    @AllArgsConstructor
    public static class OracleComment{
        private CommentType type;
        private String name;
        private String comment;
    }

    public static enum CommentType{
        COLUMN, TABLE
    }
    public static enum ModifyType {
        /**
         * 新增
         */
        ADD,
        /**
         * 修改
         */
        MODIFY
    }
}
