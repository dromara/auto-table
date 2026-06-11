package org.dromara.autotable.strategy.oracle;

import lombok.Data;
import java.util.Collections;

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
        return OracleHelper.DB.queryList(sql, Collections.emptyMap(), TabVersion.class)
                .stream()
                .filter(it -> it.getBanner() != null && it.getBanner().toLowerCase().contains("release "))
                .findFirst()
                .map(it -> {
                    try {
                        String banner = it.getBanner().toLowerCase();
                        int releaseIdx = banner.indexOf("release ");
                        if (releaseIdx < 0) {
                            return it;
                        }
                        String version = banner.substring(releaseIdx + 8);
                        int spaceIdx = version.indexOf(" ");
                        if (spaceIdx > 0) {
                            version = version.substring(0, spaceIdx);
                        }
                        int dotIdx = version.indexOf(".");
                        String mainVersion = dotIdx > 0 ? version.substring(0, dotIdx) : version;
                        it.setBanner(banner);
                        it.setVersion(version);
                        it.setMainVersion(Integer.parseInt(mainVersion));
                    } catch (Exception e) {
                        // 解析失败时保持默认值
                    }
                    return it;
                })
                .orElseGet(TabVersion::new);
    }
}
