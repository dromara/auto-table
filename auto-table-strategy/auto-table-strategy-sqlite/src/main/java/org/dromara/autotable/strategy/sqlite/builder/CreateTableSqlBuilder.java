package org.dromara.autotable.strategy.sqlite.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Slf4j
public class CreateTableSqlBuilder {

    /**
     * 构建创建新表的SQL
     * <p>CREATE TABLE "main"."无标题" -- 测试表
     * <p>(
     * <p>"id" INTEGER NOT NULL AUTOINCREMENT, -- 主键
     * <p>"name" TEXT(200) NOT NULL DEFAULT '', -- 姓名
     * <p>"age" INTEGER(2), -- 年龄
     * <p>"address" TEXT(500) DEFAULT 济南市, -- 地址
     * <p>"card_id" INTEGER(11) NOT NULL, -- 身份证id
     * <p>"card_number" text(30) NOT NULL, -- 身份证号码
     * <p>PRIMARY KEY ("id", "card_id")
     * <p>);
     */
    public static String buildTableSql(String name, String comment, List<ColumnMetadata> columnMetadataList) {
        // 获取所有主键
        List<String> primaries = new ArrayList<>();
        columnMetadataList.forEach(columnData -> {
            // 判断是主键，自动设置为NOT NULL，并记录
            if (columnData.isPrimary()) {
                columnData.setNotNull(true);
                primaries.add(columnData.getName());
            }
        });
        // 单个主键，sqlite有特殊处理，声明在列描述上，多个主键的话，像mysql一样特殊声明
        boolean isSinglePrimaryKey = primaries.size() == 1;
        boolean hasPrimaries = !primaries.isEmpty() && !isSinglePrimaryKey;

        // 记录所有修改项，（利用数组结构，便于添加,分割）
        List<String> addItems = new ArrayList<>();

        // 表字段处理
        AtomicInteger count = new AtomicInteger(0);
        addItems.add(
                columnMetadataList.stream().map(columnData -> {
                    // 拼接每个字段的sql片段,
                    // 不是最后一个字段，或者后面还有主键需要添加，加逗号
                    boolean isNotLastItem = count.incrementAndGet() < columnMetadataList.size();
                    return ColumnSqlBuilder.buildSql(columnData, isSinglePrimaryKey, isNotLastItem || hasPrimaries);
                }).collect(Collectors.joining("\n"))
        );

        // 主键
        if (hasPrimaries) {
            String primaryKeySql = getPrimaryKeySql(primaries);
            addItems.add(primaryKeySql);
        }

        // 组合sql: 过滤空字符项，逗号拼接
        String addSql = addItems.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));

        return ("CREATE TABLE {tableName}{comment} \n" +
                "(\n{addItems}\n);")
                .replace("{tableName}", name)
                .replace("{comment}", StringUtils.hasText(comment) ? " -- "  + comment : "")
                .replace("{addItems}", addSql);
    }

    /**
     * CREATE UNIQUE INDEX "main"."index_card_id"
     * ON "无标题" (
     * "card_id" ASC
     * );
     */
    public static List<String> buildIndexSql(String name, List<IndexMetadata> indexMetadataList) {
        // sqlite索引特殊处理
        // 索引
        return indexMetadataList.stream()
                .map(indexMetadata -> CreateTableSqlBuilder.getIndexSql(name, indexMetadata))
                // 同类型的索引，排在一起，SQL美化
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * CREATE INDEX "main"."index_age"
     * ON "test_test" (
     * "age" ASC,
     * "address" ASC
     * );
     */
    public static String getIndexSql(String tableName, IndexMetadata indexMetadata) {
        return StringConnectHelper.newInstance("CREATE{indexType} INDEX {indexName} ON {tableName} ({columns}) {indexComment};")
                .replace("{indexType}", indexMetadata.getType() == IndexTypeEnum.NORMAL ? "" : " " + indexMetadata.getType().name())
                .replace("{indexName}", indexMetadata.getName())
                .replace("{tableName}", tableName)
                .replace("{columns}", () -> {
                    List<IndexMetadata.IndexColumnParam> columnParams = indexMetadata.getColumns();
                    return columnParams.stream().map(column ->
                            // 例："name" ASC
                            "{column} {sortMode}"
                                    .replace("{column}", column.getColumn())
                                    .replace("{sortMode}", column.getSort() != null ? column.getSort().name() : "")
                    ).collect(Collectors.joining(","));
                })
                .replace("{indexComment}", StringUtils.hasText(indexMetadata.getComment()) ? "-- " + indexMetadata.getComment() : "")
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
