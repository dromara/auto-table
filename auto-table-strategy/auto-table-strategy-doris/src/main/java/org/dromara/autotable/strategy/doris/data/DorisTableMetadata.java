package org.dromara.autotable.strategy.doris.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.autotable.core.strategy.TableMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Getter
@Setter
@Accessors(chain = true)
public class DorisTableMetadata extends TableMetadata {

    /**
     * 所有列
     */
    private List<DorisColumnMetadata> columnMetadataList = new ArrayList<>();
    /**
     * 索引
     */
    private List<DorisIndexMetadata> indexMetadataList = new ArrayList<>();

    /**
     * 引擎
     */
    private String engine;

    /**
     * key模型类型
     */
    private String keysType;

    /**
     * keys
     */
    private List<String> keys;

    /**
     * 分区信息
     */
    private DorisPartitionMetadata partitionMetadata;

    /**
     * 分桶算法
     */
    private String distributedBy;

    /**
     * 分桶列
     */
    private List<String> distributedColumns;
    /**
     * 分桶数量
     */
    private String distributedBuckets;

    /**
     * 物化视图
     */
    private List<DorisRollupMetadata> rollupMetadataList;


    /**
     * properties
     */
    private Map<String, String> properties = new HashMap<>();

    public DorisTableMetadata(Class<?> entityClass, String tableName, String comment) {
        super(entityClass, tableName, "", comment);
    }

    public String toColumnDefinitionSql() {
        return columnMetadataList.stream()
                .map(DorisColumnMetadata::toSql)
                .collect(Collectors.joining(", "));
    }

    public CharSequence toIndexDefinitionSql() {
        if (indexMetadataList.isEmpty()) {
            return "";
        }
        return ", " + indexMetadataList.stream()
                .map(DorisIndexMetadata::toSql)
                .collect(Collectors.joining(", "));
    }

    public String toRollupDefinitionSql() {
        if (rollupMetadataList == null || rollupMetadataList.isEmpty()) {
            return "";
        }
        return "rollup ("
                + rollupMetadataList.stream()
                .map(DorisRollupMetadata::toSql)
                .collect(Collectors.joining(", "))
                + ")";
    }
}
