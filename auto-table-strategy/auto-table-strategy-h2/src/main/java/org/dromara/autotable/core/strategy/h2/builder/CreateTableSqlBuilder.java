package org.dromara.autotable.core.strategy.h2.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.h2.H2Strategy;
import org.dromara.autotable.core.strategy.h2.data.H2TypeHelper;
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
    public static List<String> buildColumnSql(DefaultTableMetadata tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        // 建表语句
        String createTableSql = getCreateTableSql(tableMetadata);

        // 创建索引语句
        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();
        List<String> createIndexSql = getCreateIndexSql(schema, tableName, indexMetadataList);

        // 为 表、字段、索引 添加注释
        List<String> addCommentSql = getAllCommentSql(tableMetadata);

        // 组合最终建表语句
        List<String> sqlList = new ArrayList<>();
        if(StringUtils.hasText(schema)) {
            sqlList.add("CREATE SCHEMA IF NOT EXISTS {schema};".replace("{schema}", schema));
            sqlList.add("SET SCHEMA {schema};".replace("{schema}", schema));
        }
        sqlList.add(createTableSql);
        // 添加提交事务，优先创建表，不然后续的字段添加注释，会找不到字段
        // sqlList.add(IStrategy.TRANSACTION_COMMIT_MARK);
        sqlList.addAll(createIndexSql);
        sqlList.addAll(addCommentSql);
        return sqlList;
    }

    /**
     * CREATE UNIQUE INDEX "uni_name" ON "表名" (
     * "name"
     * );
     */
    public static List<String> getCreateIndexSql(String schema, String tableName, List<IndexMetadata> indexMetadataList) {

        return indexMetadataList.stream()
                .map(indexMetadata -> StringConnectHelper.newInstance("CREATE {indexType} INDEX {indexName} ON {tableName} {method} ({columns});")
                        .replace("{indexType}", indexMetadata.getType() == IndexTypeEnum.UNIQUE ? "UNIQUE" : "")
                        .replace("{indexName}", indexMetadata.getName())
                        .replace("{tableName}", H2Strategy.withSchemaName(schema, tableName))
                        .replace("{method}", StringUtils.hasText(indexMetadata.getMethod()) ? "USING " + indexMetadata.getMethod() : "")
                        .replace("{columns}", () -> {
                            List<IndexMetadata.IndexColumnParam> columnParams = indexMetadata.getColumns();
                            return columnParams.stream().map(column ->
                                    // 例："name" ASC
                                    "{column} {sortMode}"
                                            .replace("{column}", column.getColumn())
                                            .replace("{sortMode}", column.getSort() != null ? column.getSort().name() : "")
                            ).collect(Collectors.joining(","));
                        })
                        .toString()
                ).collect(Collectors.toList());
    }

    private static List<String> getAllCommentSql(DefaultTableMetadata tableMetadata) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();
        String comment = tableMetadata.getComment();
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();
        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();

        return getAllCommentSql(schema, tableName, comment,
                columnMetadataList.stream().collect(Collectors.toMap(ColumnMetadata::getName, ColumnMetadata::getComment)),
                indexMetadataList.stream().collect(Collectors.toMap(IndexMetadata::getName, IndexMetadata::getComment)));
    }

    public static List<String> getAllCommentSql(String schema, String tableName, String tableComment, Map<String, String> columnCommentMap, Map<String, String> indexCommentMap) {

        List<String> commentSqlList = new ArrayList<>();

        // 表备注
        if (tableComment != null) {
            String addTableComment = StringConnectHelper.newInstance("COMMENT ON TABLE {tableName} IS {comment};")
                    .replace("{tableName}", H2Strategy.withSchemaName(schema, tableName))
                    .replace("{comment}", tableComment.isEmpty() ? "null" : "'" + tableComment + "'")
                    .toString();
            commentSqlList.add(addTableComment);
        }

        // 字段备注
        columnCommentMap.entrySet().stream()
                .map(columnComment -> StringConnectHelper.newInstance("COMMENT ON COLUMN {name} IS {comment};")
                        // ⚠️列名称转大写，不然找不到
                        .replace("{name}", H2Strategy.withSchemaName(schema, tableName, columnComment.getKey()))
                        .replace("{comment}", () -> {
                            String value = columnComment.getValue();
                            return value == null || value.isEmpty() ? "null" : "'" + value + "'";
                        })
                        .toString())
                .forEach(commentSqlList::add);

        // 索引备注
        indexCommentMap.entrySet().stream()
                .map(indexComment -> StringConnectHelper.newInstance("COMMENT ON INDEX {name} IS {comment};")
                        .replace("{name}", H2Strategy.withSchemaName(schema, indexComment.getKey()))
                        .replace("{comment}", () -> {
                            String value = indexComment.getValue();
                            return value == null || value.isEmpty() ? "null" : "'" + value + "'";
                        }).toString())
                .forEach(commentSqlList::add);

        return commentSqlList;
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
                        .map(CreateTableSqlBuilder::buildColumnSql)
                        .collect(Collectors.joining(","))
        );

        // 主键
        if (!primaries.isEmpty()) {
            String primaryKeySql = getPrimaryKeySql(primaries);
            columnList.add(primaryKeySql);
        }

        // 组合sql: 过滤空字符项，逗号拼接
        String columnSql = columnList.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));

        return "CREATE TABLE {tableName} ({columnList});"
                .replace("{tableName}", H2Strategy.withSchemaName(schema, name))
                .replace("{columnList}", columnSql);
    }


    /**
     * 生成字段相关的SQL片段
     *
     * @param columnMetadata 列元数据
     * @return 列相关的sql
     */
    public static String buildColumnSql(ColumnMetadata columnMetadata) {
        // 例子："name" varchar(100) NULL DEFAULT '张三' COMMENT '名称'
        // 例子："id" int4(32) NOT NULL AUTO_INCREMENT COMMENT '主键'
        return StringConnectHelper.newInstance("{columnName} {typeAndLength} {null} {default} {autoIncrement}")
                .replace("{columnName}", columnMetadata.getName())
                .replace("{typeAndLength}", columnMetadata.getType().getDefaultFullType())
                .replace("{autoIncrement}", columnMetadata.isAutoIncrement() ? "auto_increment" : "")
                .replace("{null}", columnMetadata.isNotNull() ? "NOT NULL" : "")
                .replace("{default}", () -> {
                    // 指定NULL
                    DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();
                    if (defaultValueType == DefaultValueEnum.NULL) {
                        return "DEFAULT NULL";
                    }
                    // 指定空字符串
                    if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
                        return "DEFAULT ''";
                    }
                    // 自定义
                    String defaultValue = columnMetadata.getDefaultValue();
                    if (DefaultValueEnum.isCustom(defaultValueType) && StringUtils.hasText(defaultValue)) {
                        // 字符串字段补单引号
                        if (!defaultValue.startsWith("'") && !defaultValue.endsWith("'") && H2TypeHelper.isCharString(columnMetadata.getType())) {
                            defaultValue = "'" + defaultValue + "'";
                        }
                        defaultValue = H2Strategy.encodeChinese(defaultValue);
                        return "DEFAULT " + defaultValue;
                    }
                    return "";
                })
                .toString();
    }

    private static String getPrimaryKeySql(List<String> primaries) {
        return "PRIMARY KEY ({primaries})"
                .replace(
                        "{primaries}",
                        String.join(",", primaries)
                );
    }
}
