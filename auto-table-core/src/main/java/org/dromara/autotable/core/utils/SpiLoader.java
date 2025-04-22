package org.dromara.autotable.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * 通用 SPI 加载工具类
 */
@Slf4j
public class SpiLoader {

    /**
     * 加载所有实现（安全，不抛异常）
     */
    public static <T> List<T> loadAll(Class<T> spiClass) {
        List<T> result = new ArrayList<>();
        try {
            ServiceLoader<T> loader = ServiceLoader.load(spiClass);
            for (T impl : loader) {
                result.add(impl);
            }
        } catch (ServiceConfigurationError e) {
            log.error("SPI 加载失败: {}", spiClass.getName(), e);
        }
        return result;
    }

    /**
     * 加载第一个实现（如果不存在，使用默认提供者）
     */
    public static <T> T loadFirst(Class<T> spiClass, Supplier<T> defaultSupplier) {
        try {
            ServiceLoader<T> loader = ServiceLoader.load(spiClass);
            Iterator<T> iterator = loader.iterator();
            if (iterator.hasNext()) {
                T impl = iterator.next();
                log.info("加载到 SPI 实现: {}", impl.getClass().getName());
                return impl;
            } else {
                log.warn("未找到 SPI 实现，使用默认实现: {}", spiClass.getName());
                return defaultSupplier.get();
            }
        } catch (ServiceConfigurationError e) {
            log.error("SPI 加载失败: {}", spiClass.getName(), e);
            return defaultSupplier.get();
        }
    }

    /**
     * 加载第一个实现（不提供默认实现，可能返回 null）
     */
    public static <T> T loadFirstOrNull(Class<T> spiClass) {
        try {
            ServiceLoader<T> loader = ServiceLoader.load(spiClass);
            Iterator<T> iterator = loader.iterator();
            if (iterator.hasNext()) {
                T impl = iterator.next();
                log.info("加载到 SPI 实现: {}", impl.getClass().getName());
                return impl;
            } else {
                log.warn("未找到 SPI 实现: {}", spiClass.getName());
                return null;
            }
        } catch (ServiceConfigurationError e) {
            log.error("SPI 加载失败: {}", spiClass.getName(), e);
            return null;
        }
    }
}
