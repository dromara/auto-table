package org.dromara.autotable.strategy.doris.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.strategy.doris.DorisHelper;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.Map;


@Slf4j
@Data
public class DorisIndexMetadata {

    private String name;

    private String column;

    private String type;

    private Map<String, String> properties;

    private String comment;

    public String toSql() {
        return StringConnectHelper.newInstance("index `{index_name}` (`{column_name}`) using {type} {properties} {comment}")
                .replace("{index_name}", name)
                .replace("{column_name}", column)
                .replace("{type}", type)
                .replace("{properties}", DorisHelper.toPropertiesSql(properties))
                .replace("{comment}", StringUtils.hasText(comment) ? "comment '" + comment + "'" : "")
                .toString();
    }
}
