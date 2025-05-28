package org.dromara.autotable.core.strategy.oracle.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class OracleSqlBuilder {


    public static List<String> createTable(DefaultTableMetadata tableMetadata) {
        List<String> list = new ArrayList<>();
        String tableName = tableMetadata.getTableName();
        String tableComment = tableMetadata.getComment();
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();
        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();
        String create = "CREATE TABLE \"{table_name}\" ({column_definition_list})"
                .replace("{table_name}", tableName)
                .replace("{column_definition_list}", toColumnDefinitionSql(columnMetadataList))
                .replaceAll(" {5}", " ")
                .replaceAll(" {4}", " ")
                .replaceAll(" {3}", " ")
                .replaceAll(" {2}", " ");
        list.add(create);
        if (StringUtils.hasText(tableComment)) {
            list.add(String.format("COMMENT ON TABLE \"%s\" IS '%s'", tableName, tableComment));
        }
        for (ColumnMetadata columnMetadata : columnMetadataList) {
            String columnName = columnMetadata.getName();
            String columnComment = columnMetadata.getComment();
            if (StringUtils.noText(columnComment)) {
                continue;
            }
            list.add(String.format("COMMENT ON COLUMN \"%s\".\"%s\" IS '%s'", tableName, columnName, columnComment));
        }
        for (IndexMetadata indexMetadata : indexMetadataList) {
            IndexTypeEnum type = indexMetadata.getType();
            String indexName = indexMetadata.getName();
            String columns = indexMetadata.getColumns()
                    .stream()
                    .map(it -> "\"" + it.getColumn() + "\"")
                    .collect(Collectors.joining(", "));
            String indexSql = StringConnectHelper.newInstance("CREATE {unique} INDEX {index_name} ON \"{table_name}\"({columns})")
                    .replace("{unique}", type == IndexTypeEnum.UNIQUE ? "UNIQUE" : "")
                    .replace("{index_name}", indexName)
                    .replace("{table_name}", tableName)
                    .replace("{columns}", columns)
                    .toString();
            list.add(indexSql);
        }
        return list;
    }

    private static String toColumnDefinitionSql(List<ColumnMetadata> columnMetadataList) {
        return columnMetadataList.stream()
                .map(OracleSqlBuilder::toColumnDefinitionSql)
                .collect(Collectors.joining(", "));
    }

    private static String toColumnDefinitionSql(ColumnMetadata columnMetadata) {
        return StringConnectHelper.newInstance("\"{column_name}\" {column_type} {primary_key} {null} {default_value}")
                .replace("{column_name}", columnMetadata.getName())
                .replace("{column_type}", columnMetadata.getType().getDefaultFullType())
                .replace("{primary_key}", columnMetadata.isPrimary() ? "PRIMARY KEY" : "")
                .replace("{null}", columnMetadata.isNotNull() ? "NOT NULL" : "")
                .replace("{default_value}", () -> {
                    DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();
                    String defaultValue = columnMetadata.getDefaultValue();
                    // 指定NULL
                    if (DefaultValueEnum.NULL.equals(defaultValueType)) {
                        return "DEFAULT NULL";
                    }
                    // 指定空字符串
                    if (DefaultValueEnum.EMPTY_STRING.equals(defaultValueType)) {
                        return "DEFAULT ''";
                    }
                    // 自定义
                    if (DefaultValueEnum.isCustom(defaultValueType) && StringUtils.hasText(defaultValue)) {
                        if ("SYSDATE".equalsIgnoreCase(defaultValue)) {
                            return "DEFAULT SYSDATE";
                        }
                        return "default '" + defaultValue + "'";
                    }
                    return "";
                })
                .toString()
                .trim();
    }
}
