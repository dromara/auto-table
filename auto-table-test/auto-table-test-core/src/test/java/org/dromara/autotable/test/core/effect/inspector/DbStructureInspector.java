package org.dromara.autotable.test.core.effect.inspector;

import java.util.List;
import java.util.Map;

/**
 * 数据库结构查询抽象接口
 * <p>
 * 用于效果测试中查询真实数据库的表结构信息，
 * 与代码生成的元数据进行比对。
 */
public interface DbStructureInspector {

    /**
     * 获取指定表的结构信息
     *
     * @param tableName 表名
     * @return 表结构信息
     */
    TableSchema getTableSchema(String tableName);

    /**
     * 获取指定表的所有列信息
     *
     * @param tableName 表名
     * @return 列信息列表
     */
    List<ColumnSchema> getColumns(String tableName);

    /**
     * 获取指定表的所有索引信息
     *
     * @param tableName 表名
     * @return 索引信息列表
     */
    List<IndexSchema> getIndexes(String tableName);

    /**
     * 获取指定表的主键信息
     *
     * @param tableName 表名
     * @return 主键信息列表
     */
    List<PrimaryKeySchema> getPrimaryKeys(String tableName);

    /**
     * 判断指定表是否存在
     *
     * @param tableName 表名
     * @return 是否存在
     */
    boolean tableExists(String tableName);

    /**
     * 表结构信息
     */
    class TableSchema {
        private String tableName;
        private String schema;
        private String comment;
        private String engine;
        private String charset;
        private String collate;
        private List<ColumnSchema> columns;
        private List<IndexSchema> indexes;
        private List<PrimaryKeySchema> primaryKeys;

        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
        public String getCharset() { return charset; }
        public void setCharset(String charset) { this.charset = charset; }
        public String getCollate() { return collate; }
        public void setCollate(String collate) { this.collate = collate; }
        public List<ColumnSchema> getColumns() { return columns; }
        public void setColumns(List<ColumnSchema> columns) { this.columns = columns; }
        public List<IndexSchema> getIndexes() { return indexes; }
        public void setIndexes(List<IndexSchema> indexes) { this.indexes = indexes; }
        public List<PrimaryKeySchema> getPrimaryKeys() { return primaryKeys; }
        public void setPrimaryKeys(List<PrimaryKeySchema> primaryKeys) { this.primaryKeys = primaryKeys; }
    }

    /**
     * 列结构信息
     */
    class ColumnSchema {
        private String name;
        private String type;
        private Integer length;
        private Integer decimalLength;
        private String comment;
        private boolean nullable;
        private String defaultValue;
        private boolean autoIncrement;
        private String charset;
        private String collate;
        private int ordinalPosition;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Integer getLength() { return length; }
        public void setLength(Integer length) { this.length = length; }
        public Integer getDecimalLength() { return decimalLength; }
        public void setDecimalLength(Integer decimalLength) { this.decimalLength = decimalLength; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public boolean isAutoIncrement() { return autoIncrement; }
        public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }
        public String getCharset() { return charset; }
        public void setCharset(String charset) { this.charset = charset; }
        public String getCollate() { return collate; }
        public void setCollate(String collate) { this.collate = collate; }
        public int getOrdinalPosition() { return ordinalPosition; }
        public void setOrdinalPosition(int ordinalPosition) { this.ordinalPosition = ordinalPosition; }
    }

    /**
     * 索引结构信息
     */
    class IndexSchema {
        private String name;
        private String type;
        private String method;
        private String comment;
        private List<String> columns;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
    }

    /**
     * 主键结构信息
     */
    class PrimaryKeySchema {
        private String name;
        private List<String> columns;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
    }
}
