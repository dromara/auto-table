package org.dromara.autotable.core.initdata;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import org.apache.commons.dbutils.QueryRunner;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.dromara.autotable.core.utils.BeanClassUtil;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // 处理默认表名的sql
        initDefaultTableSql(tableMetadata, initDataProperties);

        // 处理自定义的sql文件
        initCustomizeTableSql(tableMetadata);

        // 处理实体上特定方法返回的数据列表
        initEntityTableSql(tableMetadata);
    }

    private static void initEntityTableSql(TableMetadata tableMetadata) {
        Class<?> entityClass = tableMetadata.getEntityClass();
        Method[] declaredMethods = entityClass.getDeclaredMethods();
        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder();
        List<?> insertDataList = Arrays.stream(declaredMethods)
                // 只关注static方法
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                // 无参数的方法
                .filter(method -> method.getParameterCount() == 0)
                // 包含InitDataList注解的方法
                .filter(method -> autoTableAnnotationFinder.exist(method, InitDataList.class))
                // 方法返回值是List<Entity>的
                .filter(method -> {
                    // 第一步：确定是list
                    boolean assignableFromList = method.getReturnType().isAssignableFrom(List.class);
                    if (!assignableFromList) {
                        return false;
                    }
                    // 第二步：进一步检查泛型类型参数是否为 Entity
                    Type genericReturnType = method.getGenericReturnType();
                    if (genericReturnType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        return actualTypeArguments.length == 1 && actualTypeArguments[0] == entityClass;
                    }
                    return false;
                })
                // 调用static方法，获取方法返回值
                .map(method -> {
                    try {
                        method.setAccessible(true); // 关键步骤，绕过private限制
                        return (List<?>) method.invoke(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (!insertDataList.isEmpty()) {
            List<Field> fields = BeanClassUtil.sortAllFieldForColumn(entityClass);

            List<Map<String, Object>> rows = insertDataList.stream().map(data -> {
                Map<String, Object> map = new HashMap<>();
                fields.stream()
                        .filter(field -> TableMetadataHandler.isIncludeField(field, entityClass))
                        .forEach(field -> {
                            field.setAccessible(true);
                            String columnName = TableMetadataHandler.getColumnName(entityClass, field);
                            try {
                                Object value = field.get(data);
                                map.put(columnName, value);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        });
                return map;
            }).collect(Collectors.toList());

            List<String> columns = new ArrayList<>(rows.get(0).keySet());
            // 构建 SQL，例如：INSERT INTO user (id, name, age) VALUES (?, ?, ?)
            StringBuilder sql = new StringBuilder("INSERT INTO ")
                    .append(tableMetadata.getTableName())
                    .append(" (")
                    .append(String.join(", ", columns))
                    .append(") VALUES (")
                    .append(String.join(", ", Collections.nCopies(columns.size(), "?")))
                    .append(")");

            // 构建参数二维数组
            Object[][] params = new Object[rows.size()][columns.size()];
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                for (int j = 0; j < columns.size(); j++) {
                    params[i][j] = row.get(columns.get(j));
                }
            }

            DataSourceManager.useConnection(conn -> {
                QueryRunner runner = new QueryRunner();
                try {
                    conn.setAutoCommit(false); // 手动管理事务
                    String sqlStr = sql.toString();
                    int[] results = runner.batch(conn, sqlStr, params);
                    conn.commit(); // 提交事务
                    log.info("执行 {} 成功插入了 {} 行数据。", sqlStr, results.length);
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void initCustomizeTableSql(TableMetadata tableMetadata) {
        String initSqlFile = TableMetadataHandler.getTableInitSql(tableMetadata.getEntityClass());
        if (StringUtils.hasText(initSqlFile)) {
            try {
                String sqlContent = loadSqlContent(initSqlFile);
                log.info(">>> 执行 SQL 文件：{}", initSqlFile);
                executeSql(sqlContent);
            } catch (IOException e) {
                log.error("加载初始化SQL文件失败", e);
            } catch (Exception e) {
                log.error("执行 SQL 文件失败：{}", initSqlFile, e);
            }
        }
    }

    private static void initDefaultTableSql(TableMetadata tableMetadata, PropertyConfig.InitDataProperties initDataProperties) {
        String tableNameSqlFile;
        String basePath = initDataProperties.getBasePath();
        String datasourceName = DataSourceManager.getDatasourceName();
        if (datasourceName == null) {
            tableNameSqlFile = basePath + "/" + tableMetadata.getTableName() + ".sql";
        } else {
            tableNameSqlFile = basePath + "/" + datasourceName + "/" + tableMetadata.getTableName() + ".sql";
        }
        tryExecuteSqlFile(tableNameSqlFile);
    }

    private static void tryExecuteSqlFile(String sqlFile) {
        try {
            String sqlContent = loadSqlContent(sqlFile);
            log.info(">>> 执行 SQL 文件：{}", sqlFile);
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
