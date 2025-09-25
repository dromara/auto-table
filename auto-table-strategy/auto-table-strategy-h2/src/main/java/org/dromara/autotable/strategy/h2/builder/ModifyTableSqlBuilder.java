package org.dromara.autotable.strategy.h2.builder;

import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.h2.data.H2CompareTableInfo;
import org.dromara.autotable.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Slf4j
public class ModifyTableSqlBuilder {

    /**
     * 构建创建新表的SQL
     *
     * @param compareTableInfo 参数
     * @return sql
     */
    public static List<String> buildSql(H2CompareTableInfo compareTableInfo) {

        String tableName = compareTableInfo.getName();
        String schema = compareTableInfo.getSchema();

        String tableComment = compareTableInfo.getComment();
        Map<String, String> columnComment = compareTableInfo.getColumnComment();
        Map<String, String> indexComment = compareTableInfo.getIndexComment();

        /* 修改字段 */
        List<String> alterTableSqlList = new ArrayList<>();
        // 删除列
        List<String> dropColumnList = compareTableInfo.getDropColumnList();
        dropColumnList.stream()
                .map(columnName -> "ALTER TABLE {tableName} DROP COLUMN " + IStrategy.wrapIdentifiers(columnName))
                .forEach(alterTableSqlList::add);
        // 新增列
        List<ColumnMetadata> newColumnList = compareTableInfo.getNewColumnMetadataList();
        newColumnList.stream()
                .map(column -> "ALTER TABLE {tableName} ADD COLUMN " + CreateTableSqlBuilder.buildColumnSql(column))
                .forEach(alterTableSqlList::add);
        // 修改列
        List<ColumnMetadata> modifyColumnList = compareTableInfo.getModifyColumnMetadataList();
        modifyColumnList.stream()
                .map(column -> "ALTER TABLE {tableName} ALTER COLUMN " + CreateTableSqlBuilder.buildColumnSql(column))
                .forEach(alterTableSqlList::add);
        // 主键
        List<ColumnMetadata> newPrimaries = compareTableInfo.getNewPrimaries();
        if (!newPrimaries.isEmpty()) {
            // 删除主键
            alterTableSqlList.add("ALTER TABLE {tableName} DROP PRIMARY KEY");
            String primaryColumns = newPrimaries.stream()
                    .map(ColumnMetadata::getName)
                    .map(IStrategy::wrapIdentifiers)
                    .collect(Collectors.joining(", "));
            // 新增主键
            alterTableSqlList.add("ALTER TABLE {tableName} ADD PRIMARY KEY (" + primaryColumns + ")");
        }

        /* 为 表、字段、索引 添加注释 */
        List<String> allCommentSql = CreateTableSqlBuilder.getAllCommentSql(schema, tableName, tableComment, columnComment, indexComment);

        /* 修改 索引 */
        // 删除索引
        List<String> dropIndexList = compareTableInfo.getDropIndexList();
        List<String> dropIndexSql = dropIndexList.stream().map(indexName -> "DROP INDEX " + IStrategy.concatWrapIdentifiers(schema, indexName) + ";").collect(Collectors.toList());
        // 添加索引
        List<String> createIndexSql = CreateTableSqlBuilder.getCreateIndexSql(schema, tableName, compareTableInfo.getIndexMetadataList());

        List<String> sqlList = new ArrayList<>();
        sqlList.addAll(alterTableSqlList);
        sqlList.addAll(dropIndexSql);
        sqlList.addAll(createIndexSql);
        // 最后添加注释
        sqlList.addAll(allCommentSql);
        return sqlList.stream()
                .filter(StringUtils::hasText)
                .map(sql -> {
                    sql = sql.replace("{tableName}", IStrategy.concatWrapIdentifiers(schema, tableName));
                    if (!sql.endsWith(";")) {
                        sql += ";";
                    }
                    return sql;
                })
                .collect(Collectors.toList());
    }
}
