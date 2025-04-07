package org.dromara.autotable.core.strategy.doris.mapper;


import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaColumn;

import java.util.List;
import java.util.Map;

/**
 * 创建更新表结构的Mapper
 *
 * @author lizhian
 */
public interface DorisTablesMapper {


    @Select("select data_length from information_schema.tables where table_name = #{tableName} and table_schema = (select database())")
    Long findTableDataLength(String tableName);


    @Select("show create table `${tableName}`")
    Map<String, String> findTableCreateSql(String tableName);

    @Update("${sql}")
    int executeRawSql(String sql);

    /**
     * 根据表名查询库中该表的字段结构等信息
     *
     * @param tableName 表结构的map
     * @return 表的字段结构等信息
     */
    @Results({
            @Result(column = "character_maximum_length", property = "characterMaximumLength"),
            @Result(column = "character_octet_length", property = "characterOctetLength"),
            @Result(column = "character_set_name", property = "characterSetName"),
            @Result(column = "collation_name", property = "collationName"),
            @Result(column = "column_comment", property = "columnComment"),
            @Result(column = "column_default", property = "columnDefault"),
            @Result(column = "column_key", property = "columnKey"),
            @Result(column = "column_name", property = "columnName"),
            @Result(column = "column_type", property = "columnType"),
            @Result(column = "data_type", property = "dataType"),
            @Result(column = "datetime_precision", property = "datetimePrecision"),
            @Result(column = "extra", property = "extra"),
            @Result(column = "generation_expression", property = "generationExpression"),
            @Result(column = "is_nullable", property = "isNullable"),
            @Result(column = "numeric_precision", property = "numericPrecision"),
            @Result(column = "numeric_scale", property = "numericScale"),
            @Result(column = "ordinal_position", property = "ordinalPosition"),
            @Result(column = "privileges", property = "privileges"),
            @Result(column = "srs_id", property = "srsId"),
            @Result(column = "table_catalog", property = "tableCatalog"),
            @Result(column = "table_name", property = "tableName"),
            @Result(column = "table_schema", property = "tableSchema"),
    })
    @Select("select * from information_schema.columns where table_name = #{tableName} and table_schema = (select database()) order by ordinal_position asc")
    List<InformationSchemaColumn> findTableEnsembleByTableName(String tableName);
}
