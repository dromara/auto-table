package org.dromara.autotable.strategy.doris;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.doris.DorisDynamicPartition;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 辅助类
 *
 * @author lizhian
 */
@Slf4j
public class DorisHelper {
    private static final MessageDigest md5;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public static String generateMD5(String text) {
        byte[] hashBytes = md5.digest(text.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public static <T, K, V> Map<K, V> toMap(List<T> list, Function<T, K> keyMapping, Function<T, V> valueMapping) {
        return list.stream().collect(Collectors.toMap(keyMapping, valueMapping));
    }

    public static String getIndexName(String indexName, String column, String type) {
        String indexPrefix = AutoTableGlobalConfig.instance().getAutoTableProperties().getIndexPrefix();
        if (StringUtils.hasText(indexName)) {
            return indexPrefix + indexName;
        }
        return indexPrefix + column + "_" + type;
    }

    public static String getRollupName(String name, List<String> columns) {
        String rollupPrefix = AutoTableGlobalConfig.instance().getAutoTableProperties().getDoris().getRollupPrefix();
        int maxLength = AutoTableGlobalConfig.instance().getAutoTableProperties().getDoris().getRollupAutoNameMaxLength();
        if (StringUtils.hasText(name)) {
            return rollupPrefix + name;
        }
        String joined = String.join("_", columns);
        String rollupName = rollupPrefix + joined;
        if (rollupName.length() <= maxLength) {
            return rollupName;
        }
        String md5Str = generateMD5(joined);
        // 截取前半部分长度的字符，空余足够的位置，给“_”和MD5值
        String onePart = rollupName.substring(0, maxLength - md5Str.length());
        return onePart + md5;
    }

    public static Map<String, String> parseProperties(String[] properties) {
        Map<String, String> result = new HashMap<>();
        for (String line : properties) {
            String[] array = line.split("=");
            if (array.length < 2) {
                continue;
            }
            String key = array[0].trim();
            String value = array[1].trim();
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            result.put(key, value);

        }
        return result;
    }

    @SneakyThrows
    public static Map<String, String> getDynamicPartitionProperties(DorisDynamicPartition dynamicPartition) {
        if (!dynamicPartition.enable()) {
            return new HashMap<>();
        }
        Function<Method, String> keyMapping = method -> "dynamic_partition." + method.getName();
        Function<Method, String> valueMapping = method -> {
            try {
                return method.invoke(dynamicPartition).toString();
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
        Map<String, String> properties = Arrays.stream(DorisDynamicPartition.class.getMethods())
                .filter(it -> it.toString().contains(DorisDynamicPartition.class.getName()))
                .filter(it -> !valueMapping.apply(it).isEmpty())
                .collect(Collectors.toMap(keyMapping, valueMapping));
        properties.put("dynamic_partition.time_unit", dynamicPartition.time_unit().getValue());
        return properties;
    }

    public static String toPropertiesSql(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        String joinProperties = properties.keySet()
                .stream()
                .sorted()
                .map(key -> "\"" + key + "\" = \"" + properties.get(key) + "\"")
                .collect(Collectors.joining(", "));
        return "properties(" + joinProperties + ")";
    }


    public static String joinColumns(List<String> columns) {
        return columns.stream()
                .map(it -> "`" + it + "`")
                .collect(Collectors.joining(", "));
    }

    public static String joinValues(List<String> values) {
        return values.stream()
                .map(it -> {
                    if ("maxvalue".equalsIgnoreCase(it)) {
                        return it;
                    }
                    if ("null".equalsIgnoreCase(it)) {
                        return it;
                    }
                    return "\"" + it + "\"";
                })
                .collect(Collectors.joining(", "));
    }

    public static List<List<String>> subList(List<String> originalList, int subSize) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < originalList.size(); i += subSize) {
            // 计算子列表的起始和结束索引
            int end = Math.min(i + subSize, originalList.size());
            List<String> subList = originalList.subList(i, end);
            result.add(new ArrayList<>(subList)); // 避免原列表修改的影响
        }
        return result;
    }

    // 结果分类
    public static final String ADDED = "added";
    public static final String REMOVED = "removed";
    // 注意：这里的 "modified" 是通过检测到一个 "removed" 和一个 "added" 来 *推断* 的
    // 更精确的修改检测需要更复杂的算法（如LCS或特定SQL解析）
    // public static final String MODIFIED = "modified"; // 暂时不用，以 added/removed 为主

    /**
     * 比较两个多行SQL语句的差异
     *
     * @param sql1 第一个SQL语句 (旧版本)
     * @param sql2 第二个SQL语句 (新版本)
     * @return Map 包含 "added" 和 "removed" 的行列表.
     * Key: "added" -> List<String> 新增的行 (存在于sql2但不存在于sql1)
     * Key: "removed" -> List<String> 删除的行 (存在于sql1但不存在于sql2)
     */
    public static Map<String, List<String>> compareSqlStatements(String sql1, String sql2) {
        Map<String, List<String>> result = new HashMap<>();
        result.put(ADDED, new ArrayList<>());
        result.put(REMOVED, new ArrayList<>());
        // 1. 预处理SQL语句
        List<String> lines1 = preprocessSql(sql1);
        List<String> lines2 = preprocessSql(sql2);
        // 2. 使用 Set 进行比较，快速找出纯新增和纯删除的行
        // 使用 LinkedHashSet 保留一定的顺序性（基于预处理后的顺序）
        Set<String> set1 = new LinkedHashSet<>(lines1);
        Set<String> set2 = new LinkedHashSet<>(lines2);
        // 找出新增的行 (存在于 set2 但不存在于 set1)
        for (String line : set2) {
            if (!set1.contains(line)) {
                result.get(ADDED).add(line);
            }
        }
        // 找出删除的行 (存在于 set1 但不存在于 set2)
        for (String line : set1) {
            if (!set2.contains(line)) {
                result.get(REMOVED).add(line);
            }
        }
        // 3. (可选) 尝试识别修改
        // 简单的修改识别：如果某行被删除，同时有另一行被添加，
        // 并且它们在原始文件中的行号相近或内容相似度高，可能是一次修改。
        // 但这会显著增加复杂度，这里我们仅返回清晰的 added 和 removed。
        // 用户可以通过观察 removed 和 added 的内容来推断修改。
        return result;
    }

    /**
     * 预处理SQL语句：
     * 1. 移除注释 (单行 --, # 和多行 /* ... * /)
     * 2. 统一换行符
     * 3. 按行分割
     * 4. 对每行：
     * a. 去除首尾空格
     * b. 将内部多个空格替换为单个空格
     * c. 转换为小写 (使比较不区分大小写，注意：这会影响字符串字面量！)
     * 5. 过滤掉空行
     *
     * @param sql 原始SQL字符串
     * @return 处理后的行列表
     */
    private static List<String> preprocessSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return Collections.emptyList();
        }
        // 1. 移除注释
        // 移除多行注释 /* ... */ (非贪婪模式)
        sql = sql.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        // 移除单行注释 -- ... 和 # ...
        sql = sql.replaceAll("(--|#)[^\\n]*", ""); // 移除到行尾
        // 2. 统一换行符 (虽然 split("\\R") 能处理，显式统一更保险)
        sql = sql.replace("\r\n", "\n").replace("\r", "\n");
        // 3. 按行分割 (使用 \\R 匹配各种换行符)
        String[] lines = sql.split("\\R");
        // 4 & 5. 处理每一行并过滤
        return Arrays.stream(lines)
                .map(line -> {
                    // a. 去除首尾空格
                    String processedLine = line.trim();
                    // b. 内部多个空格替换为单个空格
                    processedLine = processedLine.replaceAll("\\s+", " ");
                    // c. 转换为小写 (重要：会影响大小写敏感的标识符或字符串！)
                    // 如果需要区分大小写，请注释掉下面这行
                    processedLine = processedLine.toLowerCase();
                    return processedLine;
                })
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    // --- 测试 Main 方法 ---
    public static void main(String[] args) {
        String sqlOld =
                "-- 用户表 Version 1\n" +
                        "CREATE TABLE users (\n" +
                        "  id INT PRIMARY KEY AUTO_INCREMENT, -- 用户ID\n" +
                        "  username VARCHAR(50) NOT NULL UNIQUE, /* 用户名 */\n" +
                        "  password VARCHAR(100) NOT NULL, # 密码\n" +
                        "  email VARCHAR(100),\n" +
                        "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                        ");\n\n" +
                        "INSERT INTO config (key, value) VALUES ('timeout', '30'); -- Set timeout";
        String sqlNew =
                "-- 用户表 Version 2\n" +
                        "CREATE TABLE users (\n" +
                        "  id INT PRIMARY KEY AUTO_INCREMENT, -- 用户唯一标识\n" +
                        "  username VARCHAR( 50 ) NOT NULL UNIQUE, -- 用户名,不允许重复\n" +
                        "  password VARCHAR(255) NOT NULL, -- 密码 (增强)\n" +
                        "  phone    VARCHAR(20), -- 新增手机号\n" +
                        "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "  updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP -- 更新时间\n" +
                        ");\n" +
                        "INSERT INTO config (key, value) VALUES ('timeout', '60'); -- Increase timeout\n" +
                        "-- New index\n" +
                        "CREATE INDEX idx_phone ON users(phone);";
        Map<String, List<String>> diff = compareSqlStatements(sqlOld, sqlNew);
        System.out.println("--- SQL 差异对比 ---");
        System.out.println("\n【新增的行】:");
        if (diff.get(ADDED).isEmpty()) {
            System.out.println("(无)");
        } else {
            diff.get(ADDED).forEach(line -> System.out.println("+ " + line));
        }
        System.out.println("\n【删除的行】:");
        if (diff.get(REMOVED).isEmpty()) {
            System.out.println("(无)");
        } else {
            diff.get(REMOVED).forEach(line -> System.out.println("- " + line));
        }
        System.out.println("\n--- 对比说明 ---");
        System.out.println("* 对比基于文本行，已进行标准化处理（去注释、去多余空格、转小写）。");
        System.out.println("* '修改'的行会表现为对应行的删除和添加。例如，`password varchar(100)` 删除，`password varchar(255)` 添加。");
        System.out.println("* 由于转换为小写，对比不区分大小写，但这可能影响SQL中的字符串字面量比较。");
    }
}
