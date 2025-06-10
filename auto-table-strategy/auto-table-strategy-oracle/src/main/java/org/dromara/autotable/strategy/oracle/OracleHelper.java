package org.dromara.autotable.strategy.oracle;


import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.DBHelper;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具
 */
public class OracleHelper {
    private static final QueryRunner queryRunner = new QueryRunner();
    /**
     * 定义一个转换处理器，用于处理数据库结果集并将其转换为Java对象
     * 这里使用了BasicRowProcessor和BeanProcessor的组合，目的是为了定制化处理某些数据类型的转换
     */
    private static final BasicRowProcessor convert = new BasicRowProcessor(new BeanProcessor() {
        /**
         * 重写processColumn方法，以应对long类型无法读取两次的转换问题
         */
        @Override
        protected Object processColumn(final ResultSet resultSet, final int index, final Class<?> propType)
                throws SQLException {
            // 获取结果集中的字符串值
            String value = resultSet.getString(index);
            // 当值非空且属性类型为Integer时，将字符串值转换为Integer
            if (StringUtils.hasText(value) && Integer.class.equals(propType)) {
                return Integer.parseInt(value);
            }
            // 对于其他情况，直接返回字符串值
            return value;
        }
    });

    public static class DB {
        /**
 * 查询返回列表
 * 使用泛型来处理不同的结果类型
 *
 * @param sql           查询的 SQL 语句
 * @param params        SQL 参数，用于替换 SQL 中的占位符
 * @param resultClass   结果集中的对象类型
 * @return              查询结果列表
 */
public static <T> List<T> queryList(String sql, Map<String, Object> params, Class<T> resultClass) {
    // 使用数据源管理器的连接执行查询
    return DataSourceManager.useConnection(connection -> {
        // 设置 SQL 参数
        String finalSql = DBHelper.setParameters(sql, params);
        try {
            // 执行查询并使用 BeanListHandler 处理结果集
            return queryRunner.query(connection, finalSql, new BeanListHandler<>(resultClass, convert));
        } catch (SQLException e) {
            // 将 SQL 异常包装为运行时异常
            throw new RuntimeException(e);
        }
    });
}


        /**
         * 根据SQL查询返回单个对象
         * 该方法用于执行一个查询，并期望得到最多一个结果该方法主要用于那些查询结果只应包含零个或一个元素的场景
         *
         * @param <T> 期望返回的对象类型，由调用者指定
         * @param sql SQL查询语句，可以包含命名参数
         * @param params 一个包含命名参数和对应值的Map，用于替换SQL语句中的命名参数
         * @param resultClass 期望返回对象的类，用于指定查询结果的类型
         * @return 返回查询结果中的第一个对象，如果没有结果则返回null
         */
        public static <T> T queryOne(String sql, Map<String, Object> params, Class<T> resultClass) {
            // 执行查询并返回结果列表
            List<T> list = queryList(sql, params, resultClass);
            // 检查查询结果是否为空或不存在
            if (list == null || list.isEmpty()) {
                return null;
            }
            // 返回查询结果中的第一个对象
            return list.get(0);
        }

    }

    public static class SQL {
        /**
         * 将列元数据转换为SQL语句片段
         * 该方法用于生成数据库列的定义语句，根据列的名称、类型、是否允许为空和默认值来构建SQL语句
         *
         * @param columnMetadata 列元数据对象，包含列的名称、类型、是否允许为空和默认值等信息
         * @return 返回构建好的列定义SQL语句片段
         */
        public static String toColumnSql(String tableName, ColumnMetadata columnMetadata) {
            // 使用StringConnectHelper构建列定义SQL语句，初始模板为"{column_name} {column_type}{default_value}{null}"
            return StringConnectHelper.newInstance("{column_name} {column_type}{default_value}{null}")
                    // 替换模板中的{column_name}为列的实际名称
                    .replace("{column_name}", columnMetadata.getName())
                    // 替换模板中的{column_type}为列的实际类型
                    .replace("{column_type}", columnMetadata.getType().getDefaultFullType())

                    // 根据列的默认值，替换模板中的{default_value}为" DEFAULT "加上默认值或空字符串
                    .replace("{default_value}", () -> {
                        // 使用OracleHelper.SQL格式化默认值
                        String defaultValue = OracleHelper.SQL.formatDefaultValue(tableName, columnMetadata);
                        // 如果格式化后的默认值存在文本，则返回" DEFAULT "加上默认值
                        if (StringUtils.hasText(defaultValue)) {
                            return " DEFAULT " + defaultValue;
                        }
                        // 如果格式化后的默认值不存在文本，则返回空字符串
                        return "";
                    })
                    // 根据列是否允许为空，替换模板中的{null}为" NOT NULL"或空字符串
                    .replace("{null}", columnMetadata.isNotNull() ? " NOT NULL" : "")
                    // 将构建好的SQL语句转换为字符串并去除多余的空格后返回
                    .toString()
                    .trim();
        }


        /**
         * 根据表名和索引元数据生成创建索引的SQL语句
         *
         * @param tableName     表名，用于指定创建索引的表
         * @param indexMetadata 索引元数据，包含索引的类型、名称和列信息
         * @return 返回创建索引的SQL语句
         */
        public static String toIndexSql(String tableName, IndexMetadata indexMetadata) {
            // 获取索引类型，用于判断是否是唯一索引
            IndexTypeEnum type = indexMetadata.getType();
            // 获取索引名称，用于SQL语句中的索引命名
            String indexName = indexMetadata.getName();

            // 根据索引列信息生成列定义部分，如果列有排序规则，则附加排序规则
            List<String> columnDefines = indexMetadata.getColumns()
                    .stream()
                    .map(it -> {
                        // 如果列没有排序规则，则仅返回列名
                        if (it.getSort() == null) {
                            return it.getColumn();
                        }
                        // 如果列有排序规则，则返回列名加排序规则
                        return it.getColumn() + " " + it.getSort().name();
                    })
                    .collect(Collectors.toList());

            // 通过StringConnectHelper构建最终的SQL语句
            return StringConnectHelper.newInstance("CREATE {unique}INDEX {index_name} ON {table_name}({columns})")
                    // 根据索引类型决定是否添加UNIQUE关键字
                    .replace("{unique}", type == IndexTypeEnum.UNIQUE ? "UNIQUE " : "")
                    // 替换SQL模板中的索引名称占位符
                    .replace("{index_name}", indexName)
                    // 替换SQL模板中的表名占位符
                    .replace("{table_name}", tableName)
                    // 替换SQL模板中的列定义占位符
                    .replace("{columns}", String.join(", ", columnDefines))
                    .toString();
        }

        /**
         * 根据列元数据格式化默认值
         * 此方法根据列的默认值类型和值，返回一个格式化后的字符串表示
         *
         * @param columnMetadata 列元数据，包含默认值类型和值
         * @return 格式化后的默认值字符串
         */
        public static String formatDefaultValue(String tableName, ColumnMetadata columnMetadata) {
            // 主键自增情况
            if (columnMetadata.isPrimary() && columnMetadata.isAutoIncrement()) {
                return "auto_seq_" + tableName + ".nextval";
            }
            // 获取列的默认值类型
            DefaultValueEnum type = columnMetadata.getDefaultValueType();
            // 获取列的默认值
            String value = columnMetadata.getDefaultValue();

            // 指定NULL
            if (DefaultValueEnum.NULL.equals(type)) {
                return "null";
            }
            // 指定空字符串
            if (DefaultValueEnum.EMPTY_STRING.equals(type)) {
                return "''";
            }
            // 未定义默认值
            if (StringUtils.noText(value)) {
                return "null";
            }

            // 获取列类型的完整定义，并转为小写
            String typeDefine = columnMetadata.getType().getDefaultFullType().toLowerCase();
            // 对于数值类型，直接返回默认值
            if (typeDefine.contains("number")
                    || typeDefine.contains("double")
                    || typeDefine.contains("float")) {
                return value;
            }

            // 特殊值SYSDATE
            if ("SYSDATE".equalsIgnoreCase(value)) {
                return "SYSDATE";
            }

            // 特殊值USER
            if ("USER".equalsIgnoreCase(value)) {
                return "USER";
            }

            // 如果默认值以单引号开头，直接返回
            if (value.startsWith("'")) {
                return value;
            }

            // 对于字符和CLOB类型，添加单引号
            if (typeDefine.contains("char") || typeDefine.contains("clob")) {
                return "'" + value + "'";
            }

            // 其他情况直接返回默认值
            return value;
        }
    }


}
