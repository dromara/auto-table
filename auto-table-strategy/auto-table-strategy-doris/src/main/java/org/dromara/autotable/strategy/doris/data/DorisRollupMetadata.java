package org.dromara.autotable.strategy.doris.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.strategy.doris.DorisHelper;
import org.dromara.autotable.core.utils.StringConnectHelper;

import java.util.List;
import java.util.Map;


@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DorisRollupMetadata {
    /**
     * 名称
     */
    private String name;

    /**
     * 字段
     */
    private List<String> columns;

    /**
     * 索引properties
     */
    private Map<String, String> properties;

    public String toSql() {
        return StringConnectHelper.newInstance("`{rollup_name}` ({columns}) {properties}")
                .replace("{rollup_name}", name)
                .replace("{columns}", DorisHelper.joinColumns(columns))
                .replace("{properties}", DorisHelper.toPropertiesSql(properties))
                .toString();
    }

}
