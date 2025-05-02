package org.dromara.autotable.solon.exception;

/**
 * 数据源不存在异常
 *
 * @author chengliang4810
 */
public class DataSourceNotFoundException extends RuntimeException {

    public DataSourceNotFoundException(final String message) {
        super(message);
    }
}
