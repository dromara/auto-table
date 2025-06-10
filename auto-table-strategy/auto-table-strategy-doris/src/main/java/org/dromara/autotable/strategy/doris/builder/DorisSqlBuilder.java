package org.dromara.autotable.strategy.doris.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.strategy.doris.DorisHelper;
import org.dromara.autotable.strategy.doris.data.DorisTableMetadata;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.stream.Collectors;

/**
 * @author don
 */
@Slf4j
public class DorisSqlBuilder {

    /**
     * 构建创建新表的SQL
     *
     * @return sql
     * <p>
     * CREATE TABLE [IF NOT EXISTS] [database.]table
     * (
     * column_definition_list
     * [, index_definition_list]
     * )
     * [engine_type]
     * [keys_type]
     * [table_comment]
     * [partition_info]
     * distribution_desc
     * [rollup_list]
     * [properties]
     * [extra_properties]
     */
    public static String buildSql(DorisTableMetadata tableMetadata) {

        String distributedColumns = tableMetadata.getDistributedColumns().stream()
                .map(column -> "`" + column + "`")
                .collect(Collectors.joining(", ", "(", ")"))
                .replace("()", "");
        String distributionDesc = "distributed by "
                + tableMetadata.getDistributedBy()
                + distributedColumns
                + " buckets "
                + tableMetadata.getDistributedBuckets();


        return "create table `{table_name}` ({column_definition_list}{index_definition_list}) engine={engine_type} {keys_type}({keys}) {table_comment} {partition_info} {distribution_desc} {rollup_list} {properties};"
                .replace("{table_name}", tableMetadata.getTableName())
                .replace("{column_definition_list}", tableMetadata.toColumnDefinitionSql())
                .replace("{index_definition_list}", tableMetadata.toIndexDefinitionSql())
                .replace("{engine_type}", tableMetadata.getEngine())
                .replace("{keys_type}", tableMetadata.getKeysType())
                .replace("{keys}", DorisHelper.joinColumns(tableMetadata.getKeys()))
                .replace("{table_comment}", StringUtils.hasText(tableMetadata.getComment())
                        ? "comment \"" + tableMetadata.getComment() + "\"" : "")
                .replace("{partition_info}", tableMetadata.getPartitionMetadata().toSql())
                .replace("{distribution_desc}", distributionDesc)
                .replace("{rollup_list}", tableMetadata.toRollupDefinitionSql())
                .replace("{properties}", DorisHelper.toPropertiesSql(tableMetadata.getProperties()))
                .replaceAll(" {5}", " ")
                .replaceAll(" {4}", " ")
                .replaceAll(" {3}", " ")
                .replaceAll(" {2}", " ")
                ;
    }


}
