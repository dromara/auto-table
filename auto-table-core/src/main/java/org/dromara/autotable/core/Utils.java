package org.dromara.autotable.core;

import org.dromara.autotable.core.utils.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static boolean tableIsExists(Connection connection, String schema, String tableName, String[] types, boolean ignoreCase) throws SQLException {

        if(ignoreCase) {
            List<String> tables = getTables(connection, schema, types);
            return tables.stream().anyMatch(name -> name.equalsIgnoreCase(tableName));
        } else {
            String catalog = connection.getCatalog();
            String realSchema = StringUtils.hasText(schema) ? schema : connection.getSchema();
            return connection.getMetaData().getTables(catalog, realSchema, tableName, types).next();
        }
    }

    public static List<String> getTables(Connection connection, String schema, String[] types) throws SQLException {
        String realSchema = StringUtils.hasText(schema) ? schema : connection.getSchema();
        String catalog = connection.getCatalog();
        ResultSet tables = connection.getMetaData().getTables(catalog, realSchema, null, types);
        List<String> tableNames = new ArrayList<>();
        while (tables.next()) {
            String existingTableName = tables.getString("TABLE_NAME");
            tableNames.add(existingTableName);
        }
        return tableNames;
    }
}
