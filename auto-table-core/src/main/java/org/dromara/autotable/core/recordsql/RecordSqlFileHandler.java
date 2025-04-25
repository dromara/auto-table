package org.dromara.autotable.core.recordsql;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class RecordSqlFileHandler implements RecordSqlHandler {
    @Override
    public void record(List<AutoTableExecuteSqlLog> autoTableExecuteSqlLogs) {

        for (AutoTableExecuteSqlLog autoTableExecuteSqlLog : autoTableExecuteSqlLogs) {

            Path path = getFilePath(autoTableExecuteSqlLog);
            if (path != null && !Files.exists(path)) {
                try {
                    Files.createDirectories(path.getParent());
                    Files.createFile(path);
                } catch (IOException e) {
                    log.error("创建日志文件{}出错", path, e);
                    path = null;
                }
            }

            if (path != null) {
                try {
                    String sqlStatement = autoTableExecuteSqlLog.getSqlStatement();
                    // 末尾添加换行符
                    if (!sqlStatement.endsWith(System.lineSeparator())) {
                        sqlStatement = sqlStatement + System.lineSeparator();
                    }
                    Files.write(path, sqlStatement.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
                } catch (IOException e) {
                    log.error("向{}写入SQL日志出错", path, e);
                }
            }
        }
    }

    /**
     * 希望自定义文件全路径的话，可以重写此方法
     */
    protected Path getFilePath(AutoTableExecuteSqlLog autoTableExecuteSqlLog) {

        PropertyConfig.RecordSqlProperties recordSql = AutoTableGlobalConfig.getAutoTableProperties().getRecordSql();

        String folderPath = recordSql.getFolderPath();

        if (StringUtils.noText(folderPath)) {
            log.error("没有指定SQL日志文件目录，无法记录SQL执行记录");
            return null;
        }

        String fileName = getFileName(autoTableExecuteSqlLog);

        return Paths.get(folderPath, fileName);
    }

    /**
     * 希望自定义文件名称的话，可以重写此方法
     */
    private String getFileName(AutoTableExecuteSqlLog autoTableExecuteSqlLog) {

        StringBuilder fileName = new StringBuilder();
        // 添加版本号
        if (StringUtils.hasText(autoTableExecuteSqlLog.getVersion())) {
            fileName.append(autoTableExecuteSqlLog.getVersion()).append("_");
        }
        // 添加数据源名称
        String dataSourceName = DataSourceManager.getDatasourceName();
        if (StringUtils.hasText(dataSourceName)) {
            fileName.append(dataSourceName).append("_");
        }
        // 添加tableSchema
        String tableSchema = autoTableExecuteSqlLog.getTableSchema();
        if (StringUtils.hasText(tableSchema)) {
            fileName.append(tableSchema).append("_");
        }
        // 添加表名
        fileName.append(autoTableExecuteSqlLog.getTableName());

        return fileName.append(".sql").toString();
    }

}
