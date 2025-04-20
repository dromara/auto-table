package org.dromara.autotable.core.strategy.mysql.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlCompareTableInfo;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Slf4j
public class ModifyTableSqlBuilder {

    /**
     * 构建创建新表的SQL
     *
     * @param mysqlCompareTableInfo 参数
     * @return sql
     */
    public static String buildSql(MysqlCompareTableInfo mysqlCompareTableInfo) {

        String name = mysqlCompareTableInfo.getName();

        String collate = mysqlCompareTableInfo.getCollate();
        String engine = mysqlCompareTableInfo.getEngine();
        String characterSet = mysqlCompareTableInfo.getCharacterSet();
        String comment = mysqlCompareTableInfo.getComment();

        List<String> dropColumnList = mysqlCompareTableInfo.getDropColumnList();
        List<MysqlCompareTableInfo.MysqlModifyColumnMetadata> modifyMysqlColumnMetadataList = mysqlCompareTableInfo.getModifyMysqlColumnMetadataList();
        List<String> dropIndexList = mysqlCompareTableInfo.getDropIndexList();
        List<IndexMetadata> mysqlIndexMetadataList = mysqlCompareTableInfo.getIndexMetadataList();

        // 记录所有修改项，（利用数组结构，便于添加,分割）
        List<String> modifyItems = new ArrayList<>();

        // 删除表字段处理
        String dropColumnSql = getDropColumnSql(dropColumnList);
        modifyItems.add(dropColumnSql);

        // 拼接每个字段的sql片段
        String columnsSql = getColumnsSql(modifyMysqlColumnMetadataList);
        modifyItems.add(columnsSql);

        /*
        处理主键
         */
        // 判断是否需要删除原有主键
        if (mysqlCompareTableInfo.isDropPrimary()) {
            modifyItems.add("DROP PRIMARY KEY");
        }
        // 判断是否存在新的主键，添加
        if (!mysqlCompareTableInfo.getNewPrimaries().isEmpty()) {
            List<String> primaries = mysqlCompareTableInfo.getNewPrimaries().stream()
                    .map(MysqlColumnMetadata::getName)
                    .collect(Collectors.toList());
            String primaryKeySql = CreateTableSqlBuilder.getPrimaryKeySql(primaries);
            modifyItems.add("ADD " + primaryKeySql);
        }

        // 删除索引
        String dropIndexSql = getDropIndexSql(dropIndexList);
        modifyItems.add(dropIndexSql);

        // 添加索引
        String addIndexSql = getAddIndexSql(mysqlIndexMetadataList);
        modifyItems.add(addIndexSql);

        // 添加表的属性
        List<String> tableProperties = CreateTableSqlBuilder.getTableProperties(engine, characterSet, collate, comment);
        modifyItems.addAll(tableProperties);

        // 组合sql: 过滤空字符项，逗号拼接
        String modifySql = modifyItems.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));

        return "ALTER TABLE `{tableName}` {modifyItems};"
                .replace("{tableName}", name)
                .replace("{modifyItems}", modifySql);
    }

    private static String getAddIndexSql(List<IndexMetadata> mysqlIndexMetadataList) {
        return mysqlIndexMetadataList.stream().map(indexParam -> {
            String indexSql = CreateTableSqlBuilder.getIndexSql(indexParam);
            return "ADD " + indexSql;
        }).collect(Collectors.joining(","));
    }

    private static String getDropIndexSql(List<String> dropIndexList) {
        return dropIndexList.stream()
                .map(dropIndex -> "DROP INDEX `{indexName}`"
                        .replace("{indexName}", dropIndex))
                .collect(Collectors.joining(","));
    }

    private static String getDropColumnSql(List<String> dropColumnList) {
        return dropColumnList.stream()
                .map(dropColumn -> "DROP COLUMN `{columnName}`"
                        .replace("{columnName}", dropColumn)
                ).collect(Collectors.joining(","));
    }

    private static String getColumnsSql(List<MysqlCompareTableInfo.MysqlModifyColumnMetadata> modifyMysqlColumnMetadataList) {
        return modifyMysqlColumnMetadataList.stream()
                .sorted(Comparator.comparingInt(modifyColumn -> modifyColumn.getMysqlColumnMetadata().getPosition()))
                .map(modifyColumn -> {
                    MysqlColumnMetadata columnMetadata = modifyColumn.getMysqlColumnMetadata();
                    // 判断是主键，自动设置为NOT NULL，并记录
                    if (columnMetadata.isPrimary()) {
                        columnMetadata.setNotNull(true);
                    }
                    String columnSql = ColumnSqlBuilder.buildSql(columnMetadata);

                    if (modifyColumn.getType() == MysqlCompareTableInfo.ModifyType.MODIFY) {
                        // 修改表字段处理
                        return "MODIFY COLUMN " + columnSql;
                    } else {
                        // 新增表字段处理
                        return "ADD COLUMN " + columnSql;
                    }
                })
                .collect(Collectors.joining(","));
    }
}
