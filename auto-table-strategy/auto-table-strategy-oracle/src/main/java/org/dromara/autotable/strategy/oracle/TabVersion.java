package org.dromara.autotable.strategy.oracle;

import lombok.Data;
import java.util.HashMap;

/**
 * 数据库版本信息
 */
@Data
public class TabVersion {
    private String banner;
    private String version;
    private int mainVersion;

    public static TabVersion search() {
        String sql = "select banner from v$version";
        return OracleHelper.DB.queryList(sql, new HashMap<>(), TabVersion.class)
                .stream()
                .filter(it -> it.getBanner().toLowerCase().contains("release "))
                .findFirst()
                .map(it -> {
                    String banner = it.getBanner().toLowerCase();
                    String version = banner.substring(banner.indexOf("release ") + 8);
                    version = version.substring(0, version.indexOf(" "));
                    String mainVersion = version.substring(0, version.indexOf("."));
                    it.setBanner(banner);
                    it.setVersion(version);
                    it.setMainVersion(Integer.parseInt(mainVersion));
                    return it;
                })
                .orElseGet(TabVersion::new);
    }
}
