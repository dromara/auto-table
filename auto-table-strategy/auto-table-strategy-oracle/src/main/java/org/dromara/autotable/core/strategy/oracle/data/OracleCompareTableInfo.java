package org.dromara.autotable.core.strategy.oracle.data;

import org.dromara.autotable.core.strategy.CompareTableInfo;

/**
 * @author don
 */
public class OracleCompareTableInfo extends CompareTableInfo {


    public OracleCompareTableInfo(String name, String schema) {
        super(name, schema);
    }

    public boolean needModify() {
        return false;
    }

    public String validateFailedMessage() {
        return "";
    }
}
