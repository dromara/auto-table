package org.dromara.autotable.solon.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * auto-table工具类
 * 替换hutool工具类
 *
 * @author chengliang4810
 */
public final class AutoTableUtils {

    private AutoTableUtils() {
        // 工具类不允许实例化
    }

    /**
     * 对象默认值处理
     * 替换 hutool ObjUtil.defaultIfNull
     */
    public static <T> T defaultIfNull(T object, T defaultValue) {
        return object != null ? object : defaultValue;
    }

    /**
     * 集合非空断言
     * 替换 hutool Assert.notEmpty
     */
    public static <T extends Collection<?>> T notEmpty(T collection, RuntimeException exception) {
        if (collection == null || collection.isEmpty()) {
            throw exception;
        }
        return collection;
    }

    /**
     * 字符串是否非空白
     * 替换 hutool StrUtil.isNotBlank
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 数组转List
     * 替换 hutool ListUtil.toList
     */
    public static List<Object> arrayToList(Object array) {
        if (array == null) {
            return Collections.emptyList();
        }

        if (array.getClass().isArray()) {
            if (array instanceof Object[]) {
                return Arrays.asList((Object[]) array);
            } else if (array instanceof int[]) {
                return Arrays.stream((int[]) array).boxed().collect(Collectors.toList());
            } else if (array instanceof long[]) {
                return Arrays.stream((long[]) array).boxed().collect(Collectors.toList());
            } else if (array instanceof double[]) {
                return Arrays.stream((double[]) array).boxed().collect(Collectors.toList());
            } else if (array instanceof float[]) {
                List<Object> result = new ArrayList<>();
                for (float f : (float[]) array) {
                    result.add(f);
                }
                return result;
            } else if (array instanceof boolean[]) {
                List<Object> result = new ArrayList<>();
                for (boolean b : (boolean[]) array) {
                    result.add(b);
                }
                return result;
            } else if (array instanceof short[]) {
                List<Object> result = new ArrayList<>();
                for (short s : (short[]) array) {
                    result.add(s);
                }
                return result;
            } else if (array instanceof byte[]) {
                List<Object> result = new ArrayList<>();
                for (byte b : (byte[]) array) {
                    result.add(b);
                }
                return result;
            } else if (array instanceof char[]) {
                List<Object> result = new ArrayList<>();
                for (char c : (char[]) array) {
                    result.add(c);
                }
                return result;
            }
        }

        return Collections.singletonList(array);
    }
}