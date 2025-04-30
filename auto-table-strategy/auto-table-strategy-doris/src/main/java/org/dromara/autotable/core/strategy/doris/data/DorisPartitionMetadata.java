package org.dromara.autotable.core.strategy.doris.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.strategy.doris.DorisHelper;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DorisPartitionMetadata {
    private boolean autoPartition;
    private String autoPartitionTimeUnit;
    private String partitionBy;
    private List<String> partitionColumns;
    private List<Partition> partitions;
    private Map<String, String> dynamicPartitionProperties;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Partition {
        private int position;
        private int columnSize;
        private String name;

        /**
         * range分区values左闭区间
         */
        private List<String> valuesLeftInclude;

        /**
         * range分区values右开区间
         */
        private List<String> valuesRightExclude;

        /**
         * range分区values分区上界
         */
        private List<String> valuesLessThan;

        /**
         * range批量分区左闭区间
         */
        private String from;

        /**
         * range批量分区右开区间
         */
        private String to;

        /**
         * range批量分区步长
         */
        private int interval;

        /**
         * range批量分区步长单位
         */
        private String unit;

        /**
         * list分区value,根据list key数量生成values in ((V1, V2,...), (Vn, Vm, ...), (...)...),
         */
        private List<String> valuesIn;

        public String toSql() {
            StringBuilder builder = new StringBuilder();
            if (StringUtils.hasText(name)) {
                builder.append("partition `").append(name).append("` ");
            }
            if (!valuesLeftInclude.isEmpty() && !valuesRightExclude.isEmpty()) {
                builder.append("values [(").append(DorisHelper.joinValues(valuesLeftInclude))
                        .append("), (")
                        .append(DorisHelper.joinValues(valuesRightExclude))
                        .append("))");
            }
            if (!valuesLessThan.isEmpty()) {
                builder.append("values less than (").append(DorisHelper.joinValues(valuesLessThan)).append(")");
            }
            if (StringUtils.hasText(from)) {
                builder.append("from (")
                        .append(StringUtils.hasText(unit) ? "\"" : "")
                        .append(from)
                        .append(StringUtils.hasText(unit) ? "\"" : "")
                        .append(") ");
            }
            if (StringUtils.hasText(to)) {
                builder.append("to (")
                        .append(StringUtils.hasText(unit) ? "\"" : "")
                        .append(to)
                        .append(StringUtils.hasText(unit) ? "\"" : "")
                        .append(") ");
            }
            if (interval > 0) {
                builder.append("interval ").append(interval).append(" ");
            }
            if (StringUtils.hasText(unit)) {
                builder.append(unit);
            }
            if (!valuesIn.isEmpty()) {
                if (columnSize == 1) {
                    builder.append("values in (").append(DorisHelper.joinValues(valuesIn)).append(")");
                }
                if (columnSize > 1) {
                    String values = DorisHelper.subList(valuesIn, columnSize)
                            .stream().filter(it -> it.size() == columnSize)
                            .map(it -> "(" + DorisHelper.joinValues(it) + ")")
                            .collect(Collectors.joining(", "));
                    builder.append("values in (").append(values).append(")");
                }
            }
            return builder.toString();

        }
    }

    public CharSequence toSql() {
        if (partitionColumns.isEmpty()) {
            return "";
        }
        String columns = DorisHelper.joinColumns(partitionColumns);
        if (autoPartition) {
            columns = "date_trunc(" + columns + ", '" + autoPartitionTimeUnit + "')";
        }
        String partition_definition_list = partitions.stream()
                .map(Partition::toSql)
                .collect(Collectors.joining(", "));

        return StringConnectHelper.newInstance("{auto} partition by {partition_By}({columns}) ({partition_definition_list})")
                .replace("{auto}", autoPartition ? "auto" : "")
                .replace("{partition_By}", partitionBy)
                .replace("{columns}", columns)
                .replace("{partition_definition_list}", partition_definition_list)
                .toString();

    }

}
