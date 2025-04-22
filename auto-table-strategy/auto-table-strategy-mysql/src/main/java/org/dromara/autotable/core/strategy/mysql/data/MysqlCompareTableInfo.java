package org.dromara.autotable.core.strategy.mysql.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IndexMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Getter
@Setter
public class MysqlCompareTableInfo extends CompareTableInfo {

    /**
     * 引擎: 有值，则说明需要修改
     */
    private String engine;
    /**
     * 默认字符集: 有值，则说明需要修改
     */
    private String characterSet;
    /**
     * 默认排序规则: 有值，则说明需要修改
     */
    private String collate;
    /**
     * 注释: 有值，则说明需要修改
     */
    private String comment;
    /**
     * 新的主键
     */
    private List<MysqlColumnMetadata> newPrimaries = new ArrayList<>();
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
    private final List<MysqlModifyColumnMetadata> modifyMysqlColumnMetadataList = new ArrayList<>();
    /**
     * 删除的索引
     */
    private final List<String> dropIndexList = new ArrayList<>();
    /**
     * 索引
     */
    private final List<IndexMetadata> indexMetadataList = new ArrayList<>();

    public MysqlCompareTableInfo(@NonNull String name, @NonNull String schema) {
        super(name, schema);
    }

    /**
     * 判断该修改参数，是不是可用，如果除了name，其他值均没有设置过，则无效，反之有效
     */
    @Override
    public boolean needModify() {
        return engine != null ||
                characterSet != null ||
                collate != null ||
                comment != null ||
                dropPrimary ||
                !newPrimaries.isEmpty() ||
                !dropColumnList.isEmpty() ||
                !modifyMysqlColumnMetadataList.isEmpty() ||
                !dropIndexList.isEmpty() ||
                !indexMetadataList.isEmpty();
    }

    @Override
    public String validateFailedMessage() {
        StringBuilder errorMsg = new StringBuilder();
        if (engine != null) {
            errorMsg.append("表引擎变更：").append(engine).append("\n");
        }
        if (characterSet != null) {
            errorMsg.append("表字符集变更：").append(characterSet).append("\n");
        }
        if (collate != null) {
            errorMsg.append("表排序规则变更：").append(collate).append("\n");
        }
        if (comment != null) {
            errorMsg.append("表注释变更：").append(comment).append("\n");
        }
        if (dropPrimary) {
            errorMsg.append("删除全部主键").append("\n");
        }
        if (!newPrimaries.isEmpty()) {
            errorMsg.append("新增主键：").append(newPrimaries.stream().map(MysqlColumnMetadata::getName).collect(Collectors.joining(","))).append("\n");
        }
        if (!dropColumnList.isEmpty()) {
            errorMsg.append("删除列：").append(String.join(",", dropColumnList)).append("\n");
        }
        if (!modifyMysqlColumnMetadataList.isEmpty()) {
            String addColumn = modifyMysqlColumnMetadataList.stream()
                    .filter(m -> m.getType() == ModifyType.ADD)
                    .map(MysqlModifyColumnMetadata::getMysqlColumnMetadata)
                    .map(MysqlColumnMetadata::getName)
                    .collect(Collectors.joining(","));
            if (!addColumn.isEmpty()) {
                errorMsg.append("新增列：").append(addColumn).append("\n");
            }
            String modifyColumn = modifyMysqlColumnMetadataList.stream()
                    .filter(m -> m.getType() == ModifyType.MODIFY)
                    .map(MysqlModifyColumnMetadata::getMysqlColumnMetadata)
                    .map(MysqlColumnMetadata::getName)
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

    public void addNewColumnMetadata(MysqlColumnMetadata mysqlColumnMetadata) {
        this.modifyMysqlColumnMetadataList.add(new MysqlModifyColumnMetadata(ModifyType.ADD, mysqlColumnMetadata));
    }

    public void addEditColumnMetadata(MysqlColumnMetadata mysqlColumnMetadata) {
        this.modifyMysqlColumnMetadataList.add(new MysqlModifyColumnMetadata(ModifyType.MODIFY, mysqlColumnMetadata));
    }

    /**
     * 重设主键
     */
    public void resetPrimary(List<MysqlColumnMetadata> primaries) {
        this.newPrimaries = primaries;
        this.dropPrimary = true;
    }

    @Data
    @AllArgsConstructor
    public static class MysqlModifyColumnMetadata {
        private ModifyType type;
        private MysqlColumnMetadata mysqlColumnMetadata;
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
