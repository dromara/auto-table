package org.dromara.autotable.core.strategy.dm.mapper;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:09
 */

import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbColumn;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbIndex;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbPrimary;

import java.util.List;
import java.util.Map;

/**
 * 达梦数据库系统表查询Mapper
 */
public interface DmTablesMapper {

    /**
     * 查询表注释
     */
    @Select("SELECT COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_NAME = #{tableName}")
    String selectTableComment(String schema, String tableName);

    /**
     * 查询表字段详细信息
     */
    @Results({
            @Result(column = "COLUMN_NAME", property = "name"),
            @Result(column = "DATA_TYPE", property = "type"),
            @Result(column = "DATA_LENGTH", property = "length"),
            @Result(column = "DATA_PRECISION", property = "precision"),
            @Result(column = "DATA_SCALE", property = "scale"),
            @Result(column = "NULLABLE", property = "nullable"),
            @Result(column = "DATA_DEFAULT", property = "defaultValue"),
            @Result(column = "COMMENTS", property = "comment")
    })
    @Select({
            "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE,",
            "       c.NULLABLE, c.DATA_DEFAULT, com.COMMENTS",
            "FROM USER_TAB_COLUMNS c",
            "LEFT JOIN USER_COL_COMMENTS com ON c.TABLE_NAME = com.TABLE_NAME AND c.COLUMN_NAME = com.COLUMN_NAME",
            "WHERE c.TABLE_NAME = #{tableName}"
    })
    List<DmDbColumn> selectTableColumns(String schema, String tableName);

    /**
     * 查询主键信息
     */
    @Results({
            @Result(column = "CONSTRAINT_NAME", property = "primaryName"),
            @Result(column = "COLUMNS", property = "columns")
    })
    @Select({
            "SELECT cons.CONSTRAINT_NAME,",
            "       LISTAGG(cols.COLUMN_NAME, ',') WITHIN GROUP (ORDER BY cols.POSITION) AS COLUMNS",
            "FROM USER_CONSTRAINTS cons",
            "JOIN USER_CONS_COLUMNS cols ON cons.CONSTRAINT_NAME = cols.CONSTRAINT_NAME",
            "WHERE cons.TABLE_NAME = #{tableName}",
            "  AND cons.CONSTRAINT_TYPE = 'P'",
            "GROUP BY cons.CONSTRAINT_NAME"
    })
    DmDbPrimary selectPrimaryKey(String schema, String tableName);

    /**
     * 查询索引信息
     */
    @Results({
            @Result(column = "INDEX_NAME", property = "indexName"),
            @Result(column = "UNIQUENESS", property = "unique"),
            @Result(column = "COLUMNS", property = "columns"),
            @Result(column = "INDEX_TYPE", property = "type")
    })
    @Select({
            "SELECT ind.INDEX_NAME,",
            "       ind.UNIQUENESS,",
            "       LISTAGG(cols.COLUMN_NAME, ',') WITHIN GROUP (ORDER BY cols.COLUMN_POSITION) AS COLUMNS,",
            "       ind.INDEX_TYPE",
            "FROM USER_INDEXES ind",
            "JOIN USER_IND_COLUMNS cols ON ind.INDEX_NAME = cols.INDEX_NAME",
            "WHERE ind.TABLE_NAME = #{tableName}",
            "  AND ind.INDEX_TYPE != 'LOB'", // 排除LOB索引
            "GROUP BY ind.INDEX_NAME, ind.UNIQUENESS, ind.INDEX_TYPE"
    })
    List<DmDbIndex> selectTableIndexes(String schema, String tableName);

    /**
     * 查询外键信息（按需实现）
     */
    @Select({
            "SELECT cons.CONSTRAINT_NAME AS foreignKeyName,",
            "       cols.COLUMN_NAME,",
            "       cons.R_CONSTRAINT_NAME AS referencedConstraint,",
            "       cons.R_OWNER AS referencedSchema,",
            "       cons.R_TABLE_NAME AS referencedTable",
            "FROM USER_CONSTRAINTS cons",
            "JOIN USER_CONS_COLUMNS cols ON cons.CONSTRAINT_NAME = cols.CONSTRAINT_NAME",
            "WHERE cons.TABLE_NAME = #{tableName}",
            "  AND cons.CONSTRAINT_TYPE = 'R'"
    })
    List<Map<String, Object>> selectForeignKeys(String schema, String tableName);
}
