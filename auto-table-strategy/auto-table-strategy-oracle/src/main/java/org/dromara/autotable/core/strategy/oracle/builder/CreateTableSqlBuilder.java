package org.dromara.autotable.core.strategy.oracle.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.oracle.data.OracleColumnMetadata;
import org.dromara.autotable.core.strategy.oracle.data.OracleTableMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Oracle建表语句构建器
 */
public class CreateTableSqlBuilder {

    /**
     * 构建Oracle建表SQL
     * @param tableMetadata 表元数据
     * @return 建表SQL
     */
    public static List<String> buildSql(OracleTableMetadata tableMetadata) {
        List<String> sql = new ArrayList<>();
        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();
        String comment = tableMetadata.getComment();
        List<OracleColumnMetadata> columns = tableMetadata.getColumnMetadataList();
        List<IndexMetadata> indexes = tableMetadata.getIndexMetadataList();

        // 1. 创建表语句
        StringBuilder createTableSb = new StringBuilder();
        createTableSb.append("CREATE TABLE ");
        if (schema != null && !schema.isEmpty()) {
            createTableSb.append(schema).append(".");
        }
        createTableSb.append(tableName).append(" (\n");

        StringJoiner columnJoiner = new StringJoiner(",\n");
        StringJoiner pkJoiner = new StringJoiner(", ");
        for (OracleColumnMetadata column : columns) {
            StringBuilder colSb = new StringBuilder();
            colSb.append(column.getName()).append(" ");
            colSb.append(column.getType().getType()).append(" ");
            if (column.getLength() != null) {
                colSb.append("(").append(column.getLength());
                if (column.getScale() != null) {
                    colSb.append(",").append(column.getScale());
                }
                colSb.append(")");
            }
            if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
                colSb.append( ColumnDefaultBuilder.build(column));
            }
            if (column.isNotNull()) {
                colSb.append(" NOT NULL");
            }
            columnJoiner.add(colSb.toString());
            if (column.isPrimary()) {
                pkJoiner.add(column.getName());
            }
        }

        createTableSb.append(columnJoiner);

        // 主键
        if (pkJoiner.length() > 0) {
            createTableSb.append(",\nCONSTRAINT PK_").append(tableName)
              .append(" PRIMARY KEY (").append(pkJoiner).append(")");
        }

        createTableSb.append("\n)");
        sql.add(createTableSb.toString());

        // 2. 表注释
        if (comment != null && !comment.isEmpty()) {
            StringBuilder tableCommentSb = new StringBuilder();
            tableCommentSb.append("COMMENT ON TABLE ");
            if (schema != null && !schema.isEmpty()) {
                tableCommentSb.append(schema).append(".");
            }
            tableCommentSb.append(tableName).append(" IS '").append(comment.replace("'", "''")).append("'");
            sql.add(tableCommentSb.toString());
        }

        // 3. 列注释
        for (OracleColumnMetadata column : columns) {
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                StringBuilder columnCommentSb = new StringBuilder();
                columnCommentSb.append("COMMENT ON COLUMN ");
                if (schema != null && !schema.isEmpty()) {
                    columnCommentSb.append(schema).append(".");
                }
                columnCommentSb.append(tableName).append(".").append(column.getName()).append(" IS '")
                  .append(column.getComment().replace("'", "''")).append("'");
                sql.add(columnCommentSb.toString());
            }
        }

        // 4. 索引
        if (indexes != null) {
            for (IndexMetadata index : indexes) {
                StringBuilder indexSb = new StringBuilder();
                indexSb.append("CREATE ");
                if (index.getType() == IndexTypeEnum.UNIQUE) {
                    indexSb.append("UNIQUE ");
                }
                indexSb.append("INDEX ").append(index.getName()).append(" ON ");
                if (schema != null && !schema.isEmpty()) {
                    indexSb.append(schema).append(".");
                }
                indexSb.append(tableName).append(" (");
                StringJoiner idxColJoiner = new StringJoiner(", ");
                for (IndexMetadata.IndexColumnParam col : index.getColumns()) {
                    idxColJoiner.add(col.getColumn());
                }
                indexSb.append(idxColJoiner).append(")");
                sql.add(indexSb.toString());
            }
        }

        return sql;
    }
}
