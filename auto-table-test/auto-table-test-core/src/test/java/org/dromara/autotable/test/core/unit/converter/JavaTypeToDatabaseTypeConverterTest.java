package org.dromara.autotable.test.core.unit.converter;

import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 测试 JavaTypeToDatabaseTypeConverter 在不同类加载器场景下的类型映射查找能力。
 * <p>
 * 问题背景：当 addTypeMapping 注册的 Class 对象与后续反射获取的 Class 对象
 * 由不同 ClassLoader 加载时（如 Spring Boot devtools 的 RestartClassLoader），
 * 使用 HashMap<Class<?>, ?> 会导致 get() 返回 null。
 * 修复方案：将 Map 的 key 从 Class<?> 改为 String（类的全限定名）。
 */
public class JavaTypeToDatabaseTypeConverterTest {

    @AfterEach
    void cleanup() {
        JavaTypeToDatabaseTypeConverter.JAVA_TO_DB_TYPE_MAPPING.clear();
    }

    /**
     * 模拟不同类加载器加载同一个类，验证类型映射仍然可以正确查找。
     */
    @Test
    public void testAddTypeMappingWithDifferentClassLoader() throws Exception {
        // 1. 使用自定义类加载器加载一个测试类
        String className = "org.dromara.autotable.test.core.unit.converter.TestEnumForConverter";
        ClassLoader customLoader = new ClassLoader(JavaTypeToDatabaseTypeConverterTest.class.getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(className)) {
                    return loadLocalClass(name);
                }
                return super.loadClass(name);
            }

            private Class<?> loadLocalClass(String name) throws ClassNotFoundException {
                String path = name.replace('.', '/') + ".class";
                try (InputStream is = getParent().getResourceAsStream(path)) {
                    if (is == null) {
                        throw new ClassNotFoundException(name);
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    byte[] classData = baos.toByteArray();
                    return defineClass(name, classData, 0, classData.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        };

        // 2. 使用自定义类加载器加载的类注册映射
        Class<?> customEnumClass = customLoader.loadClass(className);
        DefaultTypeEnumInterface mockType = new DefaultTypeEnumInterface() {
            @Override
            public String getTypeName() {
                return "VARCHAR";
            }

            @Override
            public Integer getDefaultLength() {
                return 255;
            }

            @Override
            public Integer getDefaultDecimalLength() {
                return 0;
            }
        };
        JavaTypeToDatabaseTypeConverter.addTypeMapping(DatabaseDialect.PostgreSQL, customEnumClass, mockType);

        // 3. 使用当前类加载器加载同一个类，模拟反射获取字段类型的场景
        Class<?> appEnumClass = Class.forName(className);
        assert customEnumClass != appEnumClass : "两个类加载器加载的应该是不同 Class 对象";

        // 4. 构造一个包含该字段的类，模拟框架通过反射获取字段类型
        class MockEntity {
            @SuppressWarnings("unused")
            TestEnumForConverter status;
        }
        Field field = MockEntity.class.getDeclaredField("status");

        // 5. 验证：即使 Class 对象不同，也能通过类名匹配到映射
        JavaTypeToDatabaseTypeConverter converter = new JavaTypeToDatabaseTypeConverter() {
        };
        DefaultTypeEnumInterface result = converter.getSqlType(DatabaseDialect.PostgreSQL, MockEntity.class, field);

        assertNotNull(result, "应该能匹配到自定义类型映射");
        assertEquals("VARCHAR", result.getTypeName());
    }

    /**
     * 测试 addTypeMapping(String, Map) 批量注册也能正确工作。
     */
    @Test
    public void testAddTypeMappingWithMap() {
        // 先清理已有的映射
        JavaTypeToDatabaseTypeConverter.JAVA_TO_DB_TYPE_MAPPING.clear();

        DefaultTypeEnumInterface mockType = new DefaultTypeEnumInterface() {
            @Override
            public String getTypeName() {
                return "TEXT";
            }

            @Override
            public Integer getDefaultLength() {
                return null;
            }

            @Override
            public Integer getDefaultDecimalLength() {
                return null;
            }
        };

        java.util.Map<Class<?>, DefaultTypeEnumInterface> map = new java.util.HashMap<>();
        map.put(String.class, mockType);
        JavaTypeToDatabaseTypeConverter.addTypeMapping(DatabaseDialect.PostgreSQL, map);

        // 验证注册成功
        assertEquals(1, JavaTypeToDatabaseTypeConverter.JAVA_TO_DB_TYPE_MAPPING.size());
        java.util.Map<String, DefaultTypeEnumInterface> pgMap = JavaTypeToDatabaseTypeConverter.JAVA_TO_DB_TYPE_MAPPING.get(DatabaseDialect.PostgreSQL);
        assertNotNull(pgMap);
        assertNotNull(pgMap.get("java.lang.String"));
        assertEquals("TEXT", pgMap.get("java.lang.String").getTypeName());
    }
}
