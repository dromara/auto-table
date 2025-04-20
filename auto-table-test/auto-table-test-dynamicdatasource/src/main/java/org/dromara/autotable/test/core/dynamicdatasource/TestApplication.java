package org.dromara.autotable.test.core.dynamicdatasource;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.Version;

/**
 * 多数据源测试
 * @author don
 */
public class TestApplication {

    public static void main(String[] args) {

        // 配置信息
        PropertyConfig autoTableProperties = new PropertyConfig();
        // 开启 删除不存在的列
        autoTableProperties.setAutoDropColumn(true);
        // 创建模式
        autoTableProperties.setMode(RunMode.create);

        // 记录sql
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.file);
        // 指定记录方式为PropertyConfig.RecordSqlProperties.TypeEnum.file的情况下的文件路径
        recordSqlProperties.setFolderPath("/Users/don/Downloads/sqlLogs");
        recordSqlProperties.setVersion(Version.VALUE);
        autoTableProperties.setRecordSql(recordSqlProperties);

        AutoTableGlobalConfig.setAutoTableProperties(autoTableProperties);

        // 设置数据源处理器
        AutoTableGlobalConfig.setDatasourceHandler(new DynamicDataSourceHandler());

        AutoTableBootstrap.start();
    }
}
