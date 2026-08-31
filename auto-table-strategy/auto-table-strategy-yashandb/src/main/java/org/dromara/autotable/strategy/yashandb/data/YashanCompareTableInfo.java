package org.dromara.autotable.strategy.yashandb.data;

import lombok.Getter;
import lombok.Setter;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Holds schema differences produced by the YashanDB MySQL-compatible strategy.
 */
@Getter
@Setter
public class YashanCompareTableInfo extends CompareTableInfo {
    // Difference buckets are consumed in order by YashanModifyTableSqlBuilder.
    private String comment;
    private boolean tableCommentChanged;
    private String dropPrimaryKeyName;
    private final List<ColumnMetadata> newPrimaries = new ArrayList<>();
    private final Map<String, String> columnComments = new LinkedHashMap<>();
    private final List<String> dropColumns = new ArrayList<>();
    private final Map<String, String> renameColumns = new LinkedHashMap<>();
    private final List<ColumnMetadata> modifyColumns = new ArrayList<>();
    private final List<ColumnMetadata> newColumns = new ArrayList<>();
    private final List<String> dropIndexes = new ArrayList<>();
    private final List<IndexMetadata> newIndexes = new ArrayList<>();

    public YashanCompareTableInfo(String name, String schema) {
        super(name, schema);
    }

    /**
     * Returns whether any table, column, primary-key, index, or comment change exists.
     */
    @Override
    public boolean needModify() {
        return tableCommentChanged
                || StringUtils.hasText(dropPrimaryKeyName)
                || !newPrimaries.isEmpty()
                || !columnComments.isEmpty()
                || !dropColumns.isEmpty()
                || !renameColumns.isEmpty()
                || !modifyColumns.isEmpty()
                || !newColumns.isEmpty()
                || !dropIndexes.isEmpty()
                || !newIndexes.isEmpty();
    }

    /**
     * Formats the recorded differences for AutoTable validation messages.
     */
    @Override
    public String validateFailedMessage() {
        List<String> changes = new ArrayList<>();
        // Keep each change category separate so validation output identifies the operation.
        if (tableCommentChanged) {
            changes.add("table comment");
        }
        if (StringUtils.hasText(dropPrimaryKeyName) || !newPrimaries.isEmpty()) {
            changes.add("primary key");
        }
        if (!columnComments.isEmpty()) {
            changes.add("column comments: " + String.join(",", columnComments.keySet()));
        }
        if (!dropColumns.isEmpty()) {
            changes.add("drop columns: " + String.join(",", dropColumns));
        }
        if (!renameColumns.isEmpty()) {
            changes.add("rename columns: " + renameColumns);
        }
        if (!modifyColumns.isEmpty()) {
            changes.add("modify columns: " + names(modifyColumns));
        }
        if (!newColumns.isEmpty()) {
            changes.add("new columns: " + names(newColumns));
        }
        if (!dropIndexes.isEmpty()) {
            changes.add("drop indexes: " + String.join(",", dropIndexes));
        }
        if (!newIndexes.isEmpty()) {
            changes.add("new indexes: " + newIndexes.stream().map(IndexMetadata::getName).collect(Collectors.joining(",")));
        }
        return String.join("\n", changes);
    }

    /**
     * Joins column names for a compact validation message.
     */
    private static String names(Collection<ColumnMetadata> columns) {
        return columns.stream().map(ColumnMetadata::getName).collect(Collectors.joining(","));
    }

    /**
     * Records the desired primary-key columns.
     */
    public void addNewPrimary(Collection<ColumnMetadata> columns) {
        newPrimaries.addAll(columns);
    }

    /**
     * Records columns that AutoTable may drop according to its configuration.
     */
    public void addDropColumns(Collection<String> columns) {
        dropColumns.addAll(columns);
    }

    /**
     * Records logical renames for columns that are no longer in the model.
     */
    public void addRenameColumns(Set<String> columns, String prefix) {
        columns.forEach(column -> renameColumns.put(column, prefix + column));
    }

    /**
     * Replaces a changed index by recording a drop followed by a create operation.
     */
    public void addModifyIndex(IndexMetadata index) {
        dropIndexes.add(index.getName());
        newIndexes.add(index);
    }
}
