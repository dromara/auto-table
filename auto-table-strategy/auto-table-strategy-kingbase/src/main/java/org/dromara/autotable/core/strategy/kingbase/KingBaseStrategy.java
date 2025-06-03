package org.dromara.autotable.core.strategy.kingbase;

import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.pgsql.PgsqlStrategy;
import org.dromara.autotable.core.strategy.pgsql.data.PgsqlCompareTableInfo;

/**
 * @author Min, Freddy
 * @date: 2025/3/23 20:42
 */
public class KingBaseStrategy extends PgsqlStrategy implements IStrategy<DefaultTableMetadata, PgsqlCompareTableInfo> {
    @Override
    public String databaseDialect() {
        return DatabaseDialect.KingBase;
    }

}
