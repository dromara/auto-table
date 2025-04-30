package org.dromara.autotable.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import java.beans.PropertyDescriptor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class DBHelper {

    private static QueryRunner queryRunner = new QueryRunner();
    private static BasicRowProcessor beanConvert = new BasicRowProcessor(new AnnotationBasedBeanProcessor());

    public static <T> T queryValue(Connection connection, String sql, Map<String, Object> params) {
        sql = setParameters(sql, params);
        try {
            return queryRunner.query(connection, sql, new ScalarHandler<>());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询单个结果
     *
     * @param connection  数据库连接
     * @param sql         SQL 查询语句
     * @param params      SQL 参数映射
     * @param resultClass 结果映射的实体类
     * @param <T>         返回的实体类型
     * @return 查询结果的实体类对象
     * @throws SQLException
     */
    public static <T> T queryObject(Connection connection, String sql, Map<String, Object> params, Class<T> resultClass) {
        sql = setParameters(sql, params);
        try {
            return queryRunner.query(connection, sql, new BeanHandler<>(resultClass, beanConvert));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询多个结果
     *
     * @param connection  数据库连接
     * @param sql         SQL 查询语句，支持使用:name的方式设置参数
     * @param params      SQL 参数映射
     * @param resultClass 结果映射的实体类
     * @param <T>         返回的实体类型
     * @return 查询结果的实体类对象列表
     * @throws SQLException
     */
    public static <T> List<T> queryObjectList(Connection connection, String sql, Map<String, Object> params, Class<T> resultClass) {
        // 设置 SQL 参数
        sql = setParameters(sql, params);
        try {
            return queryRunner.query(connection, sql, new BeanListHandler<>(resultClass, beanConvert));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置 SQL 参数
     *
     * @param sql    PreparedStatement
     * @param params 参数映射
     * @throws SQLException
     */
    private static String setParameters(String sql, Map<String, Object> params) {
        for (Entry<String, Object> param : params.entrySet()) {
            sql = sql.replaceAll(":" + param.getKey(), param.getValue().toString());
        }
        log.info("SQL: {}", sql);
        return sql;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ColumnName {
        String value();
    }

    // 自定义的 BeanProcessor 用于处理注解映射
    static class AnnotationBasedBeanProcessor extends BeanProcessor {

        @Override
        protected int[] mapColumnsToProperties(ResultSetMetaData rsmd, PropertyDescriptor[] props) throws SQLException {
            int cols = rsmd.getColumnCount();
            int[] columnToProperty = new int[cols + 1];

            Map<String, Integer> propertyIndexMap = new HashMap<>();
            for (int i = 0; i < props.length; i++) {
                String propName = props[i].getName();

                // 查找是否有 @ColumnName 注解
                try {
                    Field field = props[i].getReadMethod().getDeclaringClass().getDeclaredField(propName);
                    ColumnName column = field.getAnnotation(ColumnName.class);
                    if (column != null) {
                        propertyIndexMap.put(column.value().toLowerCase(), i);
                        continue;
                    }
                } catch (NoSuchFieldException ignored) {}

                // 没有注解就用属性名当列名
                propertyIndexMap.put(propName.toLowerCase(), i);
            }

            for (int col = 1; col <= cols; col++) {
                String columnName = rsmd.getColumnLabel(col);
                if (columnName == null || columnName.isEmpty()) {
                    columnName = rsmd.getColumnName(col);
                }

                Integer propIndex = propertyIndexMap.get(columnName.toLowerCase());
                if (propIndex != null) {
                    columnToProperty[col] = propIndex;
                } else {
                    columnToProperty[col] = -1;
                }
            }

            return columnToProperty;
        }
    }

}
