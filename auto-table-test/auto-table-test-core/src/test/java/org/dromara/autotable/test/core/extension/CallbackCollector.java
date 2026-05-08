package org.dromara.autotable.test.core.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 生命周期事件收集器
 * <p>
 * 用于在测试中收集和验证拦截器、Callback 的触发事件和顺序。
 */
public class CallbackCollector {

    private final List<String> events = new ArrayList<>();

    /**
     * 记录一个事件
     *
     * @param event 事件描述
     */
    public void record(String event) {
        events.add(event);
    }

    /**
     * 记录一个格式化事件
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    public void record(String format, Object... args) {
        events.add(String.format(format, args));
    }

    /**
     * 获取所有记录的事件
     *
     * @return 事件列表
     */
    public List<String> getEvents() {
        return new ArrayList<>(events);
    }

    /**
     * 清空所有记录的事件
     */
    public void clear() {
        events.clear();
    }

    /**
     * 断言事件按预期顺序触发
     *
     * @param expected 预期事件列表
     */
    public void assertEvents(String... expected) {
        assertEquals(Arrays.asList(expected), events, "Callback 触发顺序不符合预期");
    }

    /**
     * 断言事件按预期顺序触发（忽略额外事件）
     *
     * @param expected 预期事件列表
     */
    public void assertEventsContains(String... expected) {
        List<String> expectedList = Arrays.asList(expected);
        for (int i = 0; i < expectedList.size(); i++) {
            if (i >= events.size() || !events.get(i).equals(expectedList.get(i))) {
                throw new AssertionError("第 " + i + " 个事件不匹配，期望: " + expectedList.get(i) +
                        ", 实际: " + (i < events.size() ? events.get(i) : "<缺失>"));
            }
        }
    }

    /**
     * 断言包含指定事件
     *
     * @param event 预期事件
     */
    public void assertContains(String event) {
        if (!events.contains(event)) {
            throw new AssertionError("未找到预期事件: " + event + ", 实际事件: " + events);
        }
    }

    /**
     * 断言事件总数
     *
     * @param count 预期事件数
     */
    public void assertEventCount(int count) {
        assertEquals(count, events.size(), "事件数量不匹配");
    }
}
