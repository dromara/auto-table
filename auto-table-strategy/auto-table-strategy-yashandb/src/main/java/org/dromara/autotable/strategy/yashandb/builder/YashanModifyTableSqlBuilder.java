package org.dromara.autotable.strategy.yashandb.builder;

import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.yashandb.data.YashanCompareTableInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds YashanDB ALTER statements for MySQL-compatible AutoTable changes.
 */
public final class YashanModifyTableSqlBuilder {
    private YashanModifyTableSqlBuilder() {
    }

    /**
     * Builds ALTER, index, and comment statements for the recorded schema differences.
     */
    public static List<String> buildSql(YashanCompareTableInfo changes) {
        List<String> sql = new ArrayList<>();
        String table = IStrategy.concatWrapIdentifiers(changes.getSchema(), changes.getName());

        // Apply destructive index/column changes before recreating the desired definitions.
        for (String index : changes.getDropIndexes()) {
            sql.add("DROP INDEX " + IStrategy.concatWrapIdentifiers(changes.getSchema(), index));
        }
        for (ColumnMetadata column : changes.getModifyColumns()) {
            sql.add("ALTER TABLE " + table + " MODIFY " + YashanColumnSqlBuilder.buildSql(column));
        }
        if (StringUtils.hasText(changes.getDropPrimaryKeyName())) {
            sql.add("ALTER TABLE " + table + " DROP CONSTRAINT "
                    + IStrategy.wrapIdentifiers(changes.getDropPrimaryKeyName()));
        }
        for (String column : changes.getDropColumns()) {
            sql.add("ALTER TABLE " + table + " DROP COLUMN " + IStrategy.wrapIdentifiers(column));
        }
        changes.getRenameColumns().forEach((oldName, newName) -> sql.add(
                "ALTER TABLE " + table + " RENAME COLUMN " + IStrategy.wrapIdentifiers(oldName)
                        + " TO " + IStrategy.wrapIdentifiers(newName)));
        for (ColumnMetadata column : changes.getNewColumns()) {
            sql.add("ALTER TABLE " + table + " ADD " + YashanColumnSqlBuilder.buildSql(column));
        }
        if (!changes.getNewPrimaries().isEmpty()) {
            String columns = changes.getNewPrimaries().stream()
                    .map(ColumnMetadata::getName)
                    .map(IStrategy::wrapIdentifiers)
                    .collect(Collectors.joining(", "));
            sql.add("ALTER TABLE " + table + " ADD PRIMARY KEY (" + columns + ")");
        }
        // A modified index is represented as drop + create by YashanCompareTableInfo.
        changes.getNewIndexes().forEach(index -> sql.add(
                YashanCreateTableSqlBuilder.buildCreateIndex(changes.getSchema(), changes.getName(), index)));
        if (changes.isTableCommentChanged()) {
            sql.add("COMMENT ON TABLE " + table + " IS '"
                    + YashanCreateTableSqlBuilder.escape(changes.getComment()) + "'");
        }
        Map<String, String> comments = new LinkedHashMap<>();
        // Merge comments from modified and newly added columns without duplicate statements.
        changes.getModifyColumns().stream()
                .filter(column -> StringUtils.hasText(column.getComment()))
                .forEach(column -> comments.put(column.getName(), column.getComment()));
        comments.putAll(changes.getColumnComments());
        comments.forEach((column, comment) -> sql.add(
                "COMMENT ON COLUMN " + table + "." + IStrategy.wrapIdentifiers(column) + " IS '"
                        + YashanCreateTableSqlBuilder.escape(comment) + "'"));
        return sql;
    }
}
