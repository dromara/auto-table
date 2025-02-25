package org.dromara.autotable.core.strategy.dm.data;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:35
 */

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 达梦表结构对比信息
 */
@Getter
@Setter
public class DmCompareTableInfo extends CompareTableInfo {

    /**
     * 注释: 有值说明需要改
     */
    private String comment;

    /**
     * 新的主键
     */
    private List<ColumnMetadata> newPrimaries = new ArrayList<>();
    /**
     * 不为空删除主键
     */
    private String dropPrimaryKeyName;

    /**
     * 注释: 需要添加/修改的字段注释《列名，注释内容》
     */
    private Map<String, String> columnComment = new HashMap<>();

    /**
     * 注释: 需要添加/修改的索引注释《索引名，注释内容》
     */
    private Map<String, String> indexComment = new HashMap<>();

    /**
     * 需要删除的列
     */
    private List<String> dropColumnList = new ArrayList<>();

    /**
     * 需要修改的列
     */
    private List<ColumnMetadata> modifyColumnMetadataList = new ArrayList<>();

    /**
     * 需要新增的列
     */
    private List<ColumnMetadata> newColumnMetadataList = new ArrayList<>();

    /**
     * 需要删除的索引
     */
    private List<String> dropIndexList = new ArrayList<>();

    /**
     * 新添加的索引
     */
    private List<IndexMetadata> indexMetadataList = new ArrayList<>();

    /**
     * 达梦特有字段：需要重建的自增序列
     */
    private List<String> recreateSequences = new ArrayList<>();

    /**
     * 达梦特殊处理：需要修改的存储参数
     */
    private Map<String, String> storageParams = new HashMap<>();

    public DmCompareTableInfo(@NonNull String name, @NonNull String schema) {
        super(name, schema);
    }

    @Override
    public boolean needModify() {
        return StringUtils.hasText(comment) ||
                StringUtils.hasText(dropPrimaryKeyName) ||
                !newPrimaries.isEmpty() ||
                !columnComment.isEmpty() ||
                !indexComment.isEmpty() ||
                !dropColumnList.isEmpty() ||
                !modifyColumnMetadataList.isEmpty() ||
                !newColumnMetadataList.isEmpty() ||
                !dropIndexList.isEmpty() ||
                !indexMetadataList.isEmpty() ||
                !recreateSequences.isEmpty() ||
                !storageParams.isEmpty();
    }

    @Override
    public String validateFailedMessage() {
        StringBuilder dmMsg = new StringBuilder();
        if (StringUtils.hasText(comment)) {
            dmMsg.append("表注释变更: ").append(comment).append("\n");
        }
        if (StringUtils.hasText(dropPrimaryKeyName)) {
            dmMsg.append("删除主键: ").append(dropPrimaryKeyName).append("\n");
        }
        if (!newPrimaries.isEmpty()) {
            dmMsg.append("新增主键: ").append(newPrimaries).append("\n");
        }
        if (!columnComment.isEmpty()) {
            dmMsg.append("列注释变更: ").append(columnComment.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(", "))).append("\n");
        }
        if (!indexComment.isEmpty()) {
            dmMsg.append("索引注释变更: ").append(indexComment.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(", "))).append("\n");
        }
        if (!dropColumnList.isEmpty()) {
            dmMsg.append("删除列: ").append(String.join(",", dropColumnList)).append("\n");
        }
        if (!modifyColumnMetadataList.isEmpty()) {
            dmMsg.append("修改列: ").append(modifyColumnMetadataList.stream().map(ColumnMetadata::getName).collect(Collectors.joining(","))).append("\n");
        }
        if (!newColumnMetadataList.isEmpty()) {
            dmMsg.append("新增列: ").append(newColumnMetadataList.stream().map(ColumnMetadata::getName).collect(Collectors.joining(","))).append("\n");
        }
        if (!dropIndexList.isEmpty()) {
            dmMsg.append("删除索引: ").append(String.join(",", dropIndexList)).append("\n");
        }
        if (!indexMetadataList.isEmpty()) {
            dmMsg.append("新增索引: ").append(indexMetadataList.stream().map(IndexMetadata::getName).collect(Collectors.joining(","))).append("\n");
        }

        if (!recreateSequences.isEmpty()) {
            dmMsg.append("需要重建序列: ").append(String.join(",", recreateSequences)).append("\n");
        }
        if (!storageParams.isEmpty()) {
            dmMsg.append("存储参数修改: ").append(storageParams).append("\n");
        }

        return dmMsg + dmMsg.toString();
    }


    public void addColumnComment(String columnName, String newComment) {
        this.columnComment.put(columnName, newComment);
    }

    public void addNewColumn(ColumnMetadata columnMetadata) {
        this.newColumnMetadataList.add(columnMetadata);
    }

    public void addModifyColumn(ColumnMetadata columnMetadata) {
        this.modifyColumnMetadataList.add(columnMetadata);
    }

    public void addDropColumns(Set<String> dropColumnList) {
        this.dropColumnList.addAll(dropColumnList);
    }

    public void addNewIndex(IndexMetadata indexMetadata) {
        this.indexMetadataList.add(indexMetadata);
    }

    public void addModifyIndex(IndexMetadata indexMetadata) {
        this.dropIndexList.add(indexMetadata.getName());
        this.indexMetadataList.add(indexMetadata);
    }

    public void addIndexComment(@NonNull String indexName, @NonNull String newComment) {
        this.indexComment.put(indexName, newComment);
    }

    public void addDropIndexes(Set<String> indexNameList) {
        this.dropIndexList.addAll(indexNameList);
    }

    public void addNewPrimary(List<ColumnMetadata> columnMetadata) {
        this.newPrimaries.addAll(columnMetadata);
    }
}