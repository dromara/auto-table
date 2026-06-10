package org.dromara.autotable.strategy.h2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2Strategy encodeChinese() 方法单元测试
 */
public class H2EncodeChineseTest {

    @Test
    void testEncodeChinese_withPureEnglish() {
        String input = "hello world";
        String result = H2Strategy.encodeChinese(input);

        // 纯英文字符串不应该被编码
        assertEquals(input, result);
    }

    @Test
    void testEncodeChinese_withPureChinese() {
        String input = "测试";
        String result = H2Strategy.encodeChinese(input);

        // 纯中文字符串应该被编码为 Unicode 转义
        assertNotNull(result);
        assertTrue(result.startsWith("U&'"), "应该以 U&' 开头");
        assertTrue(result.endsWith("'"), "应该以 ' 结尾");
        // H2 使用 \\XXXX 格式（小写十六进制），不是 \\uXXXX
        assertTrue(result.contains("\\"), "应该包含反斜杠转义");
        // "测" = 6d4b, "试" = 8bd5
        assertTrue(result.contains("6d4b") || result.contains("8bd5"), "应该包含中文字符的十六进制编码");
    }

    @Test
    void testEncodeChinese_withMixedString() {
        String input = "hello测试world";
        String result = H2Strategy.encodeChinese(input);

        // 混合字符串中的中文应该被编码
        assertNotNull(result);
        assertTrue(result.startsWith("U&'"), "应该以 U&' 开头");
        assertTrue(result.endsWith("'"), "应该以 ' 结尾");
        assertTrue(result.contains("hello"), "应该包含英文 hello");
        assertTrue(result.contains("world"), "应该包含英文 world");
        assertTrue(result.contains("\\"), "应该包含反斜杠转义");
    }

    @Test
    void testEncodeChinese_withNull() {
        String result = H2Strategy.encodeChinese(null);

        // null 输入应该返回 null
        assertNull(result);
    }

    @Test
    void testEncodeChinese_withEmptyString() {
        String result = H2Strategy.encodeChinese("");

        // 空字符串应该返回空字符串
        assertEquals("", result);
    }

    @Test
    void testEncodeChinese_withSingleChineseChar() {
        String input = "中";
        String result = H2Strategy.encodeChinese(input);

        // 单个中文字符应该被编码
        assertNotNull(result);
        assertTrue(result.startsWith("U&'"), "应该以 U&' 开头");
        assertTrue(result.endsWith("'"), "应该以 ' 结尾");
        // "中" 的 Unicode 编码是 4e2d（小写）
        assertTrue(result.contains("4e2d"), "应该包含 '中' 的十六进制编码 4e2d");
    }

    @Test
    void testEncodeChinese_withSpecialChars() {
        String input = "测试!@#$%";
        String result = H2Strategy.encodeChinese(input);

        // 特殊字符应该保留，中文字符应该被编码
        assertNotNull(result);
        assertTrue(result.startsWith("U&'"), "应该以 U&' 开头");
        assertTrue(result.endsWith("'"), "应该以 ' 结尾");
        assertTrue(result.contains("!@#$%"), "应该保留特殊字符");
        assertTrue(result.contains("\\"), "应该包含反斜杠转义");
    }

    @Test
    void testEncodeChinese_withNumbers() {
        String input = "测试123";
        String result = H2Strategy.encodeChinese(input);

        // 数字应该保留，中文字符应该被编码
        assertNotNull(result);
        assertTrue(result.startsWith("U&'"), "应该以 U&' 开头");
        assertTrue(result.endsWith("'"), "应该以 ' 结尾");
        assertTrue(result.contains("123"), "应该保留数字");
        assertTrue(result.contains("\\"), "应该包含反斜杠转义");
    }

    @Test
    void testEncodeChinese_withAlreadyQuoted() {
        String input = "'测试'";
        String result = H2Strategy.encodeChinese(input);

        // 已经带引号的字符串不应该重复添加引号
        assertNotNull(result);
        assertTrue(result.startsWith("U&'"));
        assertTrue(result.endsWith("'"));
        // 验证不会变成 U&''测试'' 这样的格式
        assertFalse(result.contains("U&''"));
    }

    @Test
    void testEncodeChinese_withMultipleChineseChars() {
        String input = "用户名密码";
        String result = H2Strategy.encodeChinese(input);

        // 多个中文字符应该都被编码
        assertNotNull(result);
        assertTrue(result.startsWith("U&'"), "应该以 U&' 开头");
        assertTrue(result.endsWith("'"), "应该以 ' 结尾");

        // 验证每个中文字符都被编码为十六进制（小写）
        // 用 = 7528, 户 = 6237, 名 = 540d, 密 = 5bc6, 码 = 7801
        assertTrue(result.contains("7528"), "应该包含 '用' 的编码");
        assertTrue(result.contains("6237"), "应该包含 '户' 的编码");
        assertTrue(result.contains("540d"), "应该包含 '名' 的编码");
        assertTrue(result.contains("5bc6"), "应该包含 '密' 的编码");
        assertTrue(result.contains("7801"), "应该包含 '码' 的编码");
    }

    @Test
    void testEncodeChinese_preservesNonChineseUnicode() {
        String input = "café"; // 包含非中文的 Unicode 字符
        String result = H2Strategy.encodeChinese(input);

        // 非中文的 Unicode 字符（如 é）不应该被编码
        assertEquals(input, result);
    }
}
