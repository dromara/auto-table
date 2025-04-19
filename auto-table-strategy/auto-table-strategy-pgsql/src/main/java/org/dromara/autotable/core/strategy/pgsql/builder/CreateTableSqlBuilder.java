package org.dromara.autotable.core.strategy.pgsql.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.pgsql.PgsqlStrategy;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Slf4j
public class CreateTableSqlBuilder {

    /**
     * 构建创建新表的SQL
     *
     * @param tableMetadata 参数
     * @return sql
     */
    public static String buildSql(DefaultTableMetadata tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        // 建表语句
        String createTableSql = getCreateTableSql(tableMetadata);

        // 创建索引语句
        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();
        String createIndexSql = getCreateIndexSql(schema, tableName, indexMetadataList);

        // 为 表、字段、索引 添加注释
        String addCommentSql = getAddColumnCommentSql(tableMetadata);

        // 组合最终建表语句
        return createTableSql + "\n" + createIndexSql + "\n" + addCommentSql;
    }

    /**
     * CREATE UNIQUE INDEX "uni_name" ON "表名" (
     * "name"
     * );
     */
    public static String getCreateIndexSql(String schema, String tableName, List<IndexMetadata> indexMetadataList) {

        return indexMetadataList.stream()
                .map(pgsqlIndexMetadata -> getCreateIndexSql(schema, tableName, pgsqlIndexMetadata)
                ).collect(Collectors.joining("\n"));
    }

    public static String getCreateIndexSql(String schema, String tableName, IndexMetadata pgsqlIndexMetadata) {
        // 此处注意，pgsql的索引对比方式，靠的是定义索引的sql字符串整体对比的
        return StringConnectHelper.newInstance("CREATE {indexType}INDEX {indexName} ON {tableName} {method} ({columns});")
                .replace("{indexType}", pgsqlIndexMetadata.getType() == IndexTypeEnum.UNIQUE ? "UNIQUE " : "")
                .replace("{indexName}", pgsqlIndexMetadata.getName())
                .replace("{tableName}", PgsqlStrategy.withSchemaName(schema, tableName))
                .replace("{method}", getMethod(pgsqlIndexMetadata.getMethod()))
                .replace("{columns}", () -> {
                    List<IndexMetadata.IndexColumnParam> columnParams = pgsqlIndexMetadata.getColumns();
                    return columnParams.stream().map(column ->
                            // 例："name" ASC
                            "{column}{sortMode}"
                                    .replace("{column}", column.getColumn())
                                    // pgsql中，asc为默认
                                    .replace("{sortMode}", (column.getSort() == null || column.getSort() == IndexSortTypeEnum.ASC) ? "" : (" " + column.getSort().name()))
                    ).collect(Collectors.joining(", "));
                })
                .toString();
    }

    private static String getMethod(String method) {
        // 这里默认设置索引方法是为了，方便索引对比那里的对比逻辑，因为不设置方法的情况下，数据库的defsql上会默认带"USING btree"
        return "USING " + (StringUtils.hasText(method) ? method.toLowerCase() : "btree");
    }

    private static String getAddColumnCommentSql(DefaultTableMetadata tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();
        String comment = tableMetadata.getComment();
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();
        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();

        return getAddColumnCommentSql(schema, tableName, comment,
                columnMetadataList.stream().collect(Collectors.toMap(ColumnMetadata::getName, ColumnMetadata::getComment)),
                indexMetadataList.stream().collect(Collectors.toMap(IndexMetadata::getName, IndexMetadata::getComment)));
    }

    public static String getAddColumnCommentSql(String schema, String tableName, String tableComment, Map<String, String> columnCommentMap, Map<String, String> indexCommentMap) {

        String finalSchema = StringUtils.hasText(schema) ? (schema + ".") : "";
        List<String> commentList = new ArrayList<>();

        // 表备注
        if (StringUtils.hasText(tableComment)) {
            String addTableComment = "COMMENT ON TABLE {schema}{name} IS '{comment}';"
                    .replace("{schema}", finalSchema)
                    .replace("{name}", tableName)
                    .replace("{comment}", tableComment);
            commentList.add(addTableComment);
        }

        // 字段备注
        columnCommentMap.entrySet().stream()
                .map(columnComment -> "COMMENT ON COLUMN {schema}{tableName}.{name} IS '{comment}';"
                        .replace("{schema}", finalSchema)
                        .replace("{tableName}", tableName)
                        .replace("{name}", columnComment.getKey())
                        .replace("{comment}", columnComment.getValue()))
                .forEach(commentList::add);

        // 索引备注
        indexCommentMap.entrySet().stream()
                .map(indexComment -> "COMMENT ON INDEX {schema}{name} IS '{comment}';"
                        .replace("{schema}", finalSchema)
                        .replace("{name}", indexComment.getKey())
                        .replace("{comment}", indexComment.getValue()))
                .forEach(commentList::add);

        return String.join("\n", commentList);
    }

    private static String getCreateTableSql(DefaultTableMetadata tableMetadata) {

        String schema = tableMetadata.getSchema();
        String name = tableMetadata.getTableName();
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();

        // 记录所有修改项，（利用数组结构，便于添加,分割）
        List<String> columnList = new ArrayList<>();

        // 获取所有主键（至于表字段处理之前，为了主键修改notnull）
        List<String> primaries = new ArrayList<>();
        columnMetadataList.forEach(columnData -> {
            // 判断是主键，自动设置为NOT NULL，并记录
            if (columnData.isPrimary()) {
                columnData.setNotNull(true);
                primaries.add(columnData.getName());
            }
        });

        // 表字段处理
        columnList.add(
                columnMetadataList.stream()
                        // 拼接每个字段的sql片段
                        .map(ColumnSqlBuilder::buildSql)
                        .collect(Collectors.joining(","))
        );

        // 主键
        if (!primaries.isEmpty()) {
            String primaryKeySql = getPrimaryKeySql(primaries);
            columnList.add(primaryKeySql);
        }

        // 组合sql: 过滤空字符项，逗号拼接
        String addSql = columnList.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));

        return "CREATE TABLE {tableName} ({columnList});"
                .replace("{tableName}", PgsqlStrategy.withSchemaName(schema, name))
                .replace("{columnList}", addSql);
    }

    private static String getPrimaryKeySql(List<String> primaries) {
        return "PRIMARY KEY ({primaries})"
                .replace(
                        "{primaries}",
                        String.join(",", primaries)
                );
    }
}
