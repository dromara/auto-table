package org.dromara.autotable.test.core.integration;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.strategy.h2.data.H2DefaultTypeEnum;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlIndexMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlTableMetadata;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.dromara.autotable.test.core.entity.h2.TestH2;
import org.dromara.autotable.test.core.entity.mysql.custome_add_column.MyBuildTableMetadataInterceptor;
import org.dromara.autotable.test.core.entity.pgsql.TestNoColumnComment;
import org.dromara.autotable.test.core.entity.sqlite.TestSqlite;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationSingleTest extends AbstractIntegrationTest {

    @Test
    public void testMysqlColumnSort() {

        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.mysql.TestColumnSort.class
        });

        CreateTableInterceptor createTableInterceptor = (databaseDialect, tableMetadata) -> {
            assertEquals(DatabaseDialect.MySQL, databaseDialect);
            assertTrue(tableMetadata instanceof MysqlTableMetadata);
            MysqlTableMetadata mysqlTableMetadata = (MysqlTableMetadata) tableMetadata;
            List<MysqlColumnMetadata> columnMetadataList = mysqlTableMetadata.getColumnMetadataList();
            assertEquals("id", columnMetadataList.get(0).getName());
            assertEquals("update_time", columnMetadataList.get(columnMetadataList.size() - 1).getName());
        };
        AutoTableGlobalConfig.instance().setCreateTableInterceptors(Collections.singletonList(
                createTableInterceptor
        ));

        // 开始
        AutoTableBootstrap.start();
    }

    /**
     * 测试自定义添加列，是否引发表更新的问题
     */
    @Test
    public void testMysqlConsumeAddColumn() {

        initMySqlDataSource();

        AutoTableGlobalConfig.instance().setBuildTableMetadataInterceptors(Collections.singletonList(new MyBuildTableMetadataInterceptor()));
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.mysql.custome_add_column.SoftwareClassify.class
        });
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 开始
        AutoTableBootstrap.start();

        AutoTableGlobalConfig.instance().setCompareTableFinishCallbacks(
                Collections.singletonList((databaseDialect, tableMetadata, compareTableInfo) -> {
                    boolean needModify = compareTableInfo.needModify();
                    // 判断是否需要更新
                    assertFalse(needModify);
                })
        );
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testH2Create() {

        initH2DataSource();

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        autoTableProperties.setMode(RunMode.create);
        // 开启 删除不存在的列
        autoTableProperties.setAutoDropColumn(true);
        // 测试所有的公共测试类
        autoTableProperties.setModelClass(new Class[]{
                TestH2.class
        });
        // 自定义java类型与数据库类型映射关系
        JavaTypeToDatabaseTypeConverter.addTypeMapping(DatabaseDialect.H2, Date.class, H2DefaultTypeEnum.TIMESTAMP);
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testSqliteCreate() {

        initSqliteDataSource();

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        autoTableProperties.setMode(RunMode.create);
        // 测试所有的公共测试类
        autoTableProperties.setModelClass(new Class[]{
                TestSqlite.class
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testPgsqlCreate() {

        initPgSqlDataSource();

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        autoTableProperties.setMode(RunMode.update);
        // 测试所有的公共测试类
        autoTableProperties.setModelClass(new Class[]{
                TestNoColumnComment.class
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testPgsqlUpdate() {

        initPgSqlDataSource();

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        autoTableProperties.setMode(RunMode.update);
        // 测试所有的公共测试类
        autoTableProperties.setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.common_update.TestTableIndex.class
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testMysqlFullTextIndex() {

        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.mysql.TestFullTextIndex.class
        });

        CreateTableInterceptor createTableInterceptor = (databaseDialect, tableMetadata) -> {
            assertEquals(DatabaseDialect.MySQL, databaseDialect);
            assertTrue(tableMetadata instanceof MysqlTableMetadata);
            MysqlTableMetadata mysqlTableMetadata = (MysqlTableMetadata) tableMetadata;
            List<org.dromara.autotable.core.strategy.IndexMetadata> indexMetadataList = mysqlTableMetadata.getIndexMetadataList();

            // 验证生成了两个全文索引
            assertEquals(2, indexMetadataList.size());

            // 验证第一个索引是全文索引
            org.dromara.autotable.core.strategy.IndexMetadata firstIndex = indexMetadataList.get(0);
            assertTrue(firstIndex instanceof MysqlIndexMetadata);
            MysqlIndexMetadata mysqlFirstIndex = (MysqlIndexMetadata) firstIndex;
            assertTrue(mysqlFirstIndex.isFullText());
            assertEquals("content", mysqlFirstIndex.getColumns().get(0).getColumn());

            // 验证第二个索引是全文索引，并指定了分词器
            org.dromara.autotable.core.strategy.IndexMetadata secondIndex = indexMetadataList.get(1);
            assertTrue(secondIndex instanceof MysqlIndexMetadata);
            MysqlIndexMetadata mysqlSecondIndex = (MysqlIndexMetadata) secondIndex;
            assertTrue(mysqlSecondIndex.isFullText());
            assertEquals("ngram", mysqlSecondIndex.getParser());
            assertEquals("description", mysqlSecondIndex.getColumns().get(0).getColumn());
        };
        AutoTableGlobalConfig.instance().setCreateTableInterceptors(Collections.singletonList(
                createTableInterceptor
        ));

        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testMysqlTableFullTextIndex() {

        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.mysql.TestTableFullTextIndex.class
        });

        CreateTableInterceptor createTableInterceptor = (databaseDialect, tableMetadata) -> {
            assertEquals(DatabaseDialect.MySQL, databaseDialect);
            assertTrue(tableMetadata instanceof MysqlTableMetadata);
            MysqlTableMetadata mysqlTableMetadata = (MysqlTableMetadata) tableMetadata;
            List<org.dromara.autotable.core.strategy.IndexMetadata> indexMetadataList = mysqlTableMetadata.getIndexMetadataList();

            // 验证生成了一个全文索引
            assertEquals(1, indexMetadataList.size());

            // 验证是全文索引
            org.dromara.autotable.core.strategy.IndexMetadata indexMetadata = indexMetadataList.get(0);
            assertTrue(indexMetadata instanceof MysqlIndexMetadata);
            MysqlIndexMetadata mysqlIndexMetadata = (MysqlIndexMetadata) indexMetadata;
            assertTrue(mysqlIndexMetadata.isFullText());
            assertEquals("内容全文索引", mysqlIndexMetadata.getComment());
            // 验证包含两个字段
            assertEquals(2, mysqlIndexMetadata.getColumns().size());
            assertEquals("content", mysqlIndexMetadata.getColumns().get(0).getColumn());
            assertEquals("description", mysqlIndexMetadata.getColumns().get(1).getColumn());
        };
        AutoTableGlobalConfig.instance().setCreateTableInterceptors(Collections.singletonList(
                createTableInterceptor
        ));

        // 开始
        AutoTableBootstrap.start();
    }
}
