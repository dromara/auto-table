package org.dromara.autotable.core;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import org.apache.commons.dbutils.QueryRunner;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.stream.Collectors;

@Slf4j
public class InitDataHandler {

    public static void initDbData() {

        PropertyConfig.InitDataProperties initDataProperties = AutoTableGlobalConfig.instance().getAutoTableProperties().getInitData();
        if (!initDataProperties.isEnable()) {
            return;
        }

        // 处理默认sql文件
        String defaultInitSqlFile = initDataProperties.getBasePath() + "/" + initDataProperties.getDefaultInitFileName() + ".sql";
        tryExecuteSqlFile(defaultInitSqlFile);

        // 处理特定数据源名称的sql文件
        String datasourceName = DataSourceManager.getDatasourceName();
        if (StringUtils.hasText(datasourceName)) {
            String datasourceInitSqlFile = initDataProperties.getBasePath() + "/" + datasourceName + ".sql";
            tryExecuteSqlFile(datasourceInitSqlFile);
        }

    }

    public static void initTableData(TableMetadata tableMetadata) {

        PropertyConfig.InitDataProperties initDataProperties = AutoTableGlobalConfig.instance().getAutoTableProperties().getInitData();
        if (!initDataProperties.isEnable()) {
            return;
        }

        // 处理默认表明的sql
        String tableNameSqlFile;
        String basePath = initDataProperties.getBasePath();
        String datasourceName = DataSourceManager.getDatasourceName();
        if (datasourceName == null) {
            tableNameSqlFile = basePath + "/" + tableMetadata.getTableName() + ".sql";
        } else {
            tableNameSqlFile = basePath + "/" + datasourceName + "/" + tableMetadata.getTableName() + ".sql";
        }
        tryExecuteSqlFile(tableNameSqlFile);

        // 处理自定义的sql文件
        String initSqlFile = TableMetadataHandler.getTableInitSql(tableMetadata.getEntityClass());
        if (StringUtils.hasText(initSqlFile)) {
            try {
                String sqlContent = loadSqlContent(initSqlFile);
                executeSql(sqlContent);
            } catch (IOException e) {
                log.error("加载初始化SQL文件失败", e);
            } catch (Exception e) {
                log.error("执行 SQL 文件失败：{}", initSqlFile, e);
            }
        }
    }

    private static void tryExecuteSqlFile(String sqlFile) {
        try {
            String sqlContent = loadSqlContent(sqlFile);
            executeSql(sqlContent);
        } catch (FileNotFoundException ignore) {
            // 文件不存在忽略
        } catch (IOException e) {
            log.error("加载初始化SQL文件失败", e);
        } catch (Exception e) {
            log.error("执行 SQL 文件失败：{}", sqlFile, e);
        }
    }

    private static String loadSqlContent(String path) throws IOException {
        InputStream inputStream;

        if (path.startsWith("classpath:")) {
            String classpathPath = path.substring("classpath:".length());
            inputStream = InitDataHandler.class.getClassLoader().getResourceAsStream(classpathPath);
            if (inputStream == null) {
                throw new FileNotFoundException("未找到 classpath 下的文件：" + classpathPath);
            }
        } else {
            File file = new File(path);
            if (!file.exists()) {
                throw new FileNotFoundException("未找到文件路径：" + file.getAbsolutePath());
            }
            inputStream = Files.newInputStream(file.toPath());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static void executeSql(String sqlContent) {
        // IStrategy<?, ?> currentStrategy = IStrategy.getCurrentStrategy();
        // List<String> sqlList = currentStrategy.splitSql(sqlContent);
        // DataSourceManager.useConnection(conn -> {
        //
        //     // 批量的SQL 改为手动提交模式
        //     try {
        //         conn.setAutoCommit(false);
        //     } catch (SQLException e) {
        //         throw new RuntimeException(e);
        //     }
        //
        //     try (Statement stmt = conn.createStatement()) {
        //         for (String sql : sqlList) {
        //             boolean execute = stmt.execute(sql);
        //             if (execute) {
        //                 log.info("初始化SQL成功: {}", sql);
        //             } else {
        //                 ResultSet resultSet = stmt.getResultSet();
        //                 while (resultSet.next()) {
        //                     resultSet.get
        //                 }
        //                 log.warn("初始化SQL失败: {}", sql);
        //             }
        //         }
        //
        //     } catch (SQLException e) {
        //         throw new RuntimeException(e);
        //     }
        //
        //     // 提交
        //     try {
        //         conn.commit();
        //     } catch (SQLException e) {
        //         throw new RuntimeException(e);
        //     }
        // });

        try {

            // 2. 使用 JSqlParser 解析多条 SQL
            Statements statements = CCJSqlParserUtil.parseStatements(sqlContent);

            QueryRunner runner = new QueryRunner();
            // 3. 获取连接，统一使用一次事务提交
            DataSourceManager.useConnection(conn -> {
                try {
                    conn.setAutoCommit(false);
                    int count = 0;
                    for (Statement stmt : statements.getStatements()) {
                        String sql = stmt.toString().trim();
                        if (sql.isEmpty()) continue;
                        // 打印当前执行的 SQL
                        log.info(">>> 执行第 {} 条 SQL：\n{}\n", ++count, sql);
                        // 执行
                        runner.update(conn, sql);
                    }
                    conn.commit();
                    log.info(">>> 共执行 {} 条 SQL，全部提交成功！", count);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
