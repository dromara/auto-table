package org.dromara.autotable.core.strategy.oracle.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.oracle.data.OracleCompareTableInfo;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaColumn;
import org.dromara.autotable.core.utils.StringConnectHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModifyTableSqlBuilder {
    public static List<String> buildSql(OracleCompareTableInfo compareTableInfo) {
        List<String> sqlList = new ArrayList<>();
        // 根据OracleCompareTableInfo生成Oracle修改表的SQL语句
        String schema = compareTableInfo.getSchema();
        String tableName = compareTableInfo.getName();
        String fullTableName = (schema != null && !schema.isEmpty())
                ? schema + "." + tableName
                : tableName;

        // 1. 删除主键
        if (compareTableInfo.isDropPrimary()) {
            sqlList.add(StringConnectHelper.newInstance("ALTER TABLE {tableName} DROP PRIMARY KEY")
                    .replace("{tableName}", fullTableName)
                    .toString());
        }

        // 2. 新增主键
        if (compareTableInfo.getNewPrimaries() != null && !compareTableInfo.getNewPrimaries().isEmpty()) {
            sqlList.add(StringConnectHelper.newInstance("ALTER TABLE {tableName} ADD PRIMARY KEY ({columnName})")
                    .replace("{tableName}", fullTableName)
                    .replace("{columnName}", compareTableInfo.getNewPrimaries().stream().map(ColumnMetadata::getName).collect(Collectors.joining(",")))
                    .toString());
        }

        // 3. 删除列
        if (compareTableInfo.getDropColumnList() != null && !compareTableInfo.getDropColumnList().isEmpty()) {
            for (String col : compareTableInfo.getDropColumnList()) {
                sqlList.add(StringConnectHelper.newInstance("ALTER TABLE {tableName} DROP COLUMN {columnName}")
                        .replace("{tableName}", fullTableName)
                        .replace("{columnName}", col)
                        .toString());
            }
        }

        // 4. 新增/修改列
        if (compareTableInfo.getModifyOracleColumnMetadataList() != null && !compareTableInfo.getModifyOracleColumnMetadataList().isEmpty()) {
            for (OracleCompareTableInfo.OracleModifyColumnMetadata mod : compareTableInfo.getModifyOracleColumnMetadataList()) {
                String colName = mod.getOracleColumnMetadata().getName();
                String colType = mod.getOracleColumnMetadata().getType().getType();
                Integer length = mod.getOracleColumnMetadata().getLength();
                Integer scale = mod.getOracleColumnMetadata().getScale();
                boolean notNull = mod.getOracleColumnMetadata().isNotNull();
                String defaultValue = mod.getOracleColumnMetadata().getDefaultValue();
                Class<?> fieldType = mod.getOracleColumnMetadata().getFieldType();

                StringBuilder colTypeDef = new StringBuilder(colType);
                if (length != null) {
                    colTypeDef.append("(").append(length);
                    if (scale != null) {
                        colTypeDef.append(",").append(scale);
                    }
                    colTypeDef.append(")");
                }
                if (mod.getType() == OracleCompareTableInfo.ModifyType.ADD) {
                    sqlList.add(
                            StringConnectHelper.newInstance("ALTER TABLE {tableName} ADD {columnName} {dataType} {defaultValue} {notNull}")
                                    .replace("{tableName}", fullTableName)
                                    .replace("{columnName}", colName)
                                    .replace("{dataType}", colTypeDef.toString())
                                    .replace("{defaultValue}", ColumnDefaultBuilder.defStr(fieldType, defaultValue))
                                    .replace("{notNull}", notNull ? "NOT NULL" : "NULL")
                                    .toString()
                    );
                } else {
                    InformationSchemaColumn originalColumn = mod.getOracleColumnMetadata().getOriginalColumn();
                    boolean notNullChange = notNull != originalColumn.isNotNull();
                    boolean defaultValueChange = ColumnDefaultBuilder.equals(fieldType, defaultValue, originalColumn.getDataDefault());
                    sqlList.add(StringConnectHelper.newInstance("ALTER TABLE {tableName} MODIFY {columnName} {dataType} {defaultValue} {notNull}")
                            .replace("{tableName}", fullTableName)
                            .replace("{columnName}", colName)
                            .replace("{dataType}", colTypeDef.toString())
                            .replace("{defaultValue}", defaultValueChange ? ColumnDefaultBuilder.defStr(fieldType, defaultValue) : "")
                            .replace("{notNull}", notNullChange ? (notNull ? "NOT NULL" : "NULL") : "")
                            .toString());
                }
            }
        }

        // 5. 删除索引
        if (compareTableInfo.getDropIndexList() != null && !compareTableInfo.getDropIndexList().isEmpty()) {
            for (String idx : compareTableInfo.getDropIndexList()) {
                sqlList.add(
                        StringConnectHelper.newInstance("DROP INDEX {indexName}")
                                .replace("{indexName}", idx)
                                .toString()
                );
            }
        }

        // 6. 新增索引
        if (compareTableInfo.getIndexMetadataList() != null && !compareTableInfo.getIndexMetadataList().isEmpty()) {
            for (org.dromara.autotable.core.strategy.IndexMetadata index : compareTableInfo.getIndexMetadataList()) {
                String sql = StringConnectHelper.newInstance("CREATE {type} INDEX {indexName} ON {tableName} ({columnName})")
                        .replace("{type}", index.getType() != null && index.getType() == IndexTypeEnum.UNIQUE ? "UNIQUE" : "")
                        .replace("{indexName}", index.getName())
                        .replace("{tableName}", fullTableName)
                        .replace("{columnName}", index.getColumns().stream().map(it ->
                                it.getColumn() + " " + (it.getSort() == null ? "" : it.getSort().toString()))
                                .collect(Collectors.joining(",")))
                        .toString();
                sqlList.add(sql);
            }
        }
        // 7. 修改注释
        if (!compareTableInfo.getCommentList().isEmpty()) {
            for (OracleCompareTableInfo.OracleComment oracleComment : compareTableInfo.getCommentList()) {
                String sql = StringConnectHelper.newInstance("COMMENT ON {type} {name} IS '{comment}'")
                        .replace("{type}", oracleComment.getType().toString())
                        .replace("{name}", oracleComment.getName())
                        .replace("{comment}", oracleComment.getComment().replace("'", "''"))
                        .toString();
                sqlList.add(sql);
            }
        }

        return sqlList;
    }
}
