package org.dromara.autotable.test.core.entity.mysql.custome_add_column;

import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.interceptor.BuildTableMetadataInterceptor;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlTableMetadata;

import java.util.ArrayList;
import java.util.List;


/**
 * @Description 增强AutoTable生成表元数据拦截器，支持多租户方式扫描自动加载租户id
 *
 * @author qian_gs
 * @since 2025/4/25 16:23
 */
public class MyBuildTableMetadataInterceptor implements BuildTableMetadataInterceptor {
    /**
     * 拦截器
     * @param databaseDialect 数据库方言：MySQL、PostgreSQL、SQLite
     * @param tableMetadata 表元数据：MysqlTableMetadata、DefaultTableMetadata
     */
    public void intercept(final String databaseDialect, final TableMetadata tableMetadata) {
        // DatabaseDialect.MYSQL是框架内置常量，可以直接使用
        if (DatabaseDialect.MySQL.equals(databaseDialect)) {
            MysqlTableMetadata mysqlTableMetadata = (MysqlTableMetadata) tableMetadata;
            // 获取类的Class对象
            Class<?> clazz = mysqlTableMetadata.getEntityClass();
            // 获取类上的注解
            if (clazz.isAnnotationPresent(TenantTable.class)) {
                System.out.println("租户表["+mysqlTableMetadata.getTableName()+"]通过@TenantTable注解拦截装载租户id");
                //添加租户字段
                List<MysqlColumnMetadata> columnMetadataList = mysqlTableMetadata.getColumnMetadataList();
                MysqlColumnMetadata mysqlColumnMetadata =  new MysqlColumnMetadata();
                mysqlColumnMetadata.setPosition(columnMetadataList.size());
                mysqlColumnMetadata.setName("tenant_id")
                        .setComment("所属租户id")
                        .setNotNull(true)
                        .setType(new DatabaseTypeAndLength(MysqlTypeConstant.BIGINT, 20,null,new ArrayList<>()))
                        .setDefaultValue("0");
                //加载到AutoTable扫描的实体类中
                columnMetadataList.add(mysqlColumnMetadata);
            }
        }
    }
}
