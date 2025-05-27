package org.dromara.autotable.core.strategy.oracle;

import lombok.NonNull;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.oracle.data.OracleCompareTableInfo;
import org.dromara.autotable.core.strategy.oracle.data.OracleTableMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OracleStrategy implements IStrategy<OracleTableMetadata, OracleCompareTableInfo> {


    @Override
    public String databaseDialect() {
        return DatabaseDialect.Oracle;
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return new HashMap<>();
    }

    @Override
    public String dropTable(String schema, String tableName) {
        return "";
    }

    @Override
    public @NonNull OracleTableMetadata analyseClass(Class<?> beanClass) {
        return new OracleTableMetadata(beanClass, null, null, null);
    }

    @Override
    public List<String> createTable(OracleTableMetadata tableMetadata) {
        return new ArrayList<>();
    }

    @Override
    public OracleCompareTableInfo compareTable(OracleTableMetadata tableMetadata) {
        return new OracleCompareTableInfo(tableMetadata.getTableName(), tableMetadata.getSchema());
    }

    @Override
    public List<String> modifyTable(OracleCompareTableInfo compareTableInfo) {
        return new ArrayList<>();
    }


}
