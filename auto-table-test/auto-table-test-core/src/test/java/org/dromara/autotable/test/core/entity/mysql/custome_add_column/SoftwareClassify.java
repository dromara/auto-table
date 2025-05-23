package org.dromara.autotable.test.core.entity.mysql.custome_add_column;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

/**
 * @author qian_gs
 * @Description 应用分类表
 * @since 2025-04-23 09:07:09
 */
@Getter
@Setter
@TenantTable
@Accessors(chain = true)
@AutoTable(value = "software_classify", comment = "应用分类表")
public class SoftwareClassify {

    /**
     * 主键id
     */
    @PrimaryKey
    @AutoColumn(value = "id", comment = "主键id", type = MysqlTypeConstant.VARCHAR, length = 64, notNull = true)
    private String id;

    /**
     * 排序号
     */
    @AutoColumn(value = "order_no", comment = "排序号", type = MysqlTypeConstant.VARCHAR, length = 32)
    private String orderNo;

    /**
     * 名称
     */
    @AutoColumn(value = "classify_name", comment = "名称", type = MysqlTypeConstant.VARCHAR, length = 255)
    private String classifyName;

    /**
     * 编码
     */
    @AutoColumn(value = "classify_no", comment = "编码", type = MysqlTypeConstant.VARCHAR, length = 255)
    private String classifyNo;

    /**
     * 描述
     */
    @AutoColumn(value = "classify_description", comment = "描述", type = MysqlTypeConstant.VARCHAR, length = 255)
    private String classifyDescription;

    /**
     * 图标
     */
    @AutoColumn(value = "classify_icon", comment = "图标", type = MysqlTypeConstant.VARCHAR, length = 255)
    private String classifyIcon;

    /**
     * 删除状态->DelStatusEnum
     */
    @AutoColumn(value = "del_status", comment = "删除状态", type = MysqlTypeConstant.CHAR, length = 1)
    private String delStatus;

    /**
     * 删除状态枚举
     */
    public enum DelStatusEnum {

        /**
         * 已删除
         */
        DELETED("1", "已删除"),
        /**
         * 未删除
         */
        NOT_DELETED("2", "未删除");

        private final String code;
        private final String description;

        DelStatusEnum(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

}
