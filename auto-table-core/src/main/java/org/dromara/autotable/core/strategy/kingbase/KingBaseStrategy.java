package org.dromara.autotable.core.strategy.kingbase;

import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.strategy.pgsql.PgsqlStrategy;

/**
 * @author Min, Freddy
 * @date: 2025/3/23 20:42
 */
public class KingBaseStrategy extends PgsqlStrategy {
    @Override
    public String databaseDialect() {
        return DatabaseDialect.KingBase;
    }

}
