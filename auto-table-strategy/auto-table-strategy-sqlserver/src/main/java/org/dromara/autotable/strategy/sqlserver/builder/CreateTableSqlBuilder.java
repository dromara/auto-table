package org.dromara.autotable.strategy.sqlserver.builder;

import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 建表 SQL 生成器（SQLServer）。
 *
 * <p>注意：SQLServer JDBC 默认 {@code Statement.execute} 只执行单条语句，因此本类返回 {@code List<String>}，
 * 每个元素为一条独立语句（CREATE TABLE / CREATE INDEX / EXEC sp_addextendedproperty），
 * 由 core 的 {@code executeSql} 逐条执行。</p>
 *
 * @author don
 */
public class CreateTableSqlBuilder {

    private CreateTableSqlBuilder() {
    }

    /**
     * 构建创建新表的全部 SQL（建表 + 索引 + 注释）。
     *
     * @param tableMetadata 表元数据
     * @return sql 列表，每条为独立语句
     */
    public static List<String> buildSql(DefaultTableMetadata tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        List<String> sqlList = new ArrayList<>();

        // 建表语句
        sqlList.add(getCreateTableSql(tableMetadata));

        // 创建索引语句
        sqlList.addAll(getCreateIndexSql(schema, tableName, tableMetadata.getIndexMetadataList()));

        // 为 表、字段、索引 添加注释
        sqlList.addAll(getAddColumnCommentSql(tableMetadata));

        return sqlList;
    }

    /**
     * 生成 CREATE TABLE 语句。
     * <p>主键以无名 PRIMARY KEY 约束声明，SQLServer 自动生成约束名；改表时通过 mapper 查询该名进行 drop。</p>
     */
    public static String getCreateTableSql(DefaultTableMetadata tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();

        // 主键列强制 NOT NULL，并记录主键列名
        List<String> primaries = new ArrayList<>();
        columnMetadataList.forEach(columnData -> {
            if (columnData.isPrimary()) {
                columnData.setNotNull(true);
                primaries.add(columnData.getName());
            }
        });

        // 字段定义
        List<String> columnList = new ArrayList<>();
        columnList.add(columnMetadataList.stream()
                .map(ColumnSqlBuilder::buildSql)
                .collect(Collectors.joining(", ")));

        // 主键
        if (!primaries.isEmpty()) {
            columnList.add("PRIMARY KEY (" + IStrategy.customConcatWrapIdentifiers(", ", primaries) + ")");
        }

        String columnsSql = String.join(", ", columnList.stream().filter(StringUtils::hasText).collect(Collectors.toList()));

        return "CREATE TABLE " + IStrategy.concatWrapIdentifiers(schema, tableName) + " (" + columnsSql + ")";
    }

    /**
     * 生成多个索引的 CREATE INDEX 语句。
     */
    public static List<String> getCreateIndexSql(String schema, String tableName, List<IndexMetadata> indexMetadataList) {

        return indexMetadataList.stream()
                .map(indexMetadata -> getCreateIndexSql(schema, tableName, indexMetadata))
                .collect(Collectors.toList());
    }

    /**
     * 生成单个索引的 CREATE INDEX 语句。
     * <p>SQLServer 索引默认为非聚集 B-Tree，无需 USING 子句。</p>
     */
    public static String getCreateIndexSql(String schema, String tableName, IndexMetadata indexMetadata) {

        String indexType = indexMetadata.getType() == IndexTypeEnum.UNIQUE ? "UNIQUE " : "";
        String columns = indexMetadata.getColumns().stream()
                .map(column -> IStrategy.wrapIdentifiers(column.getColumn())
                        + (column.getSort() == null || column.getSort() == IndexSortTypeEnum.ASC ? "" : " " + column.getSort().name()))
                .collect(Collectors.joining(", "));

        return "CREATE " + indexType + "INDEX " + IStrategy.wrapIdentifiers(indexMetadata.getName())
                + " ON " + IStrategy.concatWrapIdentifiers(schema, tableName) + " (" + columns + ")";
    }

    /**
     * 为 表、字段、索引 添加注释（建表场景，注释一定不存在，全用 sp_addextendedproperty）。
     */
    public static List<String> getAddColumnCommentSql(DefaultTableMetadata tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();
        String comment = tableMetadata.getComment();
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();
        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();

        return getAddColumnCommentSql(schema, tableName, comment,
                columnMetadataList.stream()
                        .filter(col -> StringUtils.hasText(col.getComment()))
                        .collect(Collectors.toMap(ColumnMetadata::getName, ColumnMetadata::getComment)),
                indexMetadataList.stream()
                        .filter(idx -> StringUtils.hasText(idx.getComment()))
                        .collect(Collectors.toMap(IndexMetadata::getName, IndexMetadata::getComment)));
    }

    /**
     * 生成表/列/索引的 sp_addextendedproperty 注释语句列表。
     *
     * @param schema           schema
     * @param tableName        表名
     * @param tableComment     表注释
     * @param columnCommentMap 列注释（列名 -> 注释）
     * @param indexCommentMap  索引注释（索引名 -> 注释）
     * @return 注释语句列表
     */
    public static List<String> getAddColumnCommentSql(String schema, String tableName, String tableComment,
                                                     Map<String, String> columnCommentMap, Map<String, String> indexCommentMap) {

        List<String> commentList = new ArrayList<>();

        // 表注释
        if (StringUtils.hasText(tableComment)) {
            commentList.add(getAddExtendedPropertySql(tableComment, schema, tableName, null));
        }

        // 列注释
        columnCommentMap.forEach((columnName, columnComment) ->
                commentList.add(getAddExtendedPropertySql(columnComment, schema, tableName, columnName)));

        // 索引注释
        indexCommentMap.forEach((indexName, indexComment) ->
                commentList.add(getAddExtendedPropertySql(indexComment, schema, tableName, indexName, true)));

        return commentList;
    }

    /**
     * 生成单条 sp_addextendedproperty 语句（表或列注释）。
     *
     * @param comment     注释内容
     * @param schema      schema 名
     * @param tableName   表名
     * @param columnName  列名，为 null 表示表注释
     * @return sp_addextendedproperty 语句
     */
    public static String getAddExtendedPropertySql(String comment, String schema, String tableName, String columnName) {

        String valueLiteral = "N'" + escape(comment) + "'";
        StringBuilder sql = new StringBuilder("EXEC sp_addextendedproperty")
                .append(" @name=N'MS_Description', @value=").append(valueLiteral)
                .append(", @level0type=N'SCHEMA', @level0name=N'").append(escape(schema)).append("'")
                .append(", @level1type=N'TABLE', @level1name=N'").append(escape(tableName)).append("'");
        if (columnName != null) {
            sql.append(", @level2type=N'COLUMN', @level2name=N'").append(escape(columnName)).append("'");
        }
        return sql.toString();
    }

    /**
     * 生成单条 sp_addextendedproperty 语句（索引注释）。
     *
     * @param comment   注释内容
     * @param schema    schema 名
     * @param tableName 表名
     * @param indexName 索引名
     * @return sp_addextendedproperty 语句
     */
    public static String getAddExtendedPropertySql(String comment, String schema, String tableName, String indexName, boolean isIndex) {

        return "EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'" + escape(comment) + "'"
                + ", @level0type=N'SCHEMA', @level0name=N'" + escape(schema) + "'"
                + ", @level1type=N'TABLE', @level1name=N'" + escape(tableName) + "'"
                + ", @level2type=N'INDEX', @level2name=N'" + escape(indexName) + "'";
    }

    /**
     * 生成单条 sp_updateextendedproperty 语句（更新已存在的注释），结构与 sp_addextendedproperty 一致。
     */
    public static String getUpdateExtendedPropertySql(String comment, String schema, String tableName, String columnName) {

        String valueLiteral = "N'" + escape(comment) + "'";
        StringBuilder sql = new StringBuilder("EXEC sp_updateextendedproperty")
                .append(" @name=N'MS_Description', @value=").append(valueLiteral)
                .append(", @level0type=N'SCHEMA', @level0name=N'").append(escape(schema)).append("'")
                .append(", @level1type=N'TABLE', @level1name=N'").append(escape(tableName)).append("'");
        if (columnName != null) {
            sql.append(", @level2type=N'COLUMN', @level2name=N'").append(escape(columnName)).append("'");
        }
        return sql.toString();
    }

    public static String getUpdateExtendedPropertySql(String comment, String schema, String tableName, String indexName, boolean isIndex) {

        return "EXEC sp_updateextendedproperty @name=N'MS_Description', @value=N'" + escape(comment) + "'"
                + ", @level0type=N'SCHEMA', @level0name=N'" + escape(schema) + "'"
                + ", @level1type=N'TABLE', @level1name=N'" + escape(tableName) + "'"
                + ", @level2type=N'INDEX', @level2name=N'" + escape(indexName) + "'";
    }

    /**
     * 单引号转义为两个单引号。
     */
    private static String escape(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
