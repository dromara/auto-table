package org.dromara.autotable.test.core.unit.utils;

import org.dromara.autotable.core.utils.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringUtils 单元测试
 */
public class StringUtilsTest {

    @Test
    void testHasText_withNonEmptyString() {
        assertTrue(StringUtils.hasText("hello"));
    }

    @Test
    void testHasText_withEmptyString() {
        assertFalse(StringUtils.hasText(""));
    }

    @Test
    void testHasText_withNull() {
        assertFalse(StringUtils.hasText(null));
    }

    @Test
    void testHasText_withWhitespaceOnly() {
        assertFalse(StringUtils.hasText("   "));
    }

    @Test
    void testNoText_withEmptyString() {
        assertTrue(StringUtils.noText(""));
    }

    @Test
    void testNoText_withNonEmptyString() {
        assertFalse(StringUtils.noText("hello"));
    }

    @Test
    void testCamelToUnderline_simple() {
        assertEquals("user_name", StringUtils.camelToUnderline("userName"));
    }

    @Test
    void testCamelToUnderline_multipleWords() {
        assertEquals("user_name_age", StringUtils.camelToUnderline("userNameAge"));
    }

    @Test
    void testCamelToUnderline_startsWithUpperCase() {
        assertEquals("user_name", StringUtils.camelToUnderline("UserName"));
    }

    @Test
    void testCamelToUnderline_allLowerCase() {
        assertEquals("username", StringUtils.camelToUnderline("username"));
    }

    @Test
    void testCamelToUnderline_empty() {
        assertEquals("", StringUtils.camelToUnderline(""));
    }
}
