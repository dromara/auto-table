package com.tangzc.autotable.core.strategy.mysql.builder;

import com.tangzc.autotable.annotation.ColumnDefault;
import com.tangzc.autotable.annotation.ColumnType;
import com.tangzc.autotable.annotation.mysql.MysqlColumnCharset;
import com.tangzc.autotable.core.AutoTableGlobalConfig;
import com.tangzc.autotable.core.builder.ColumnMetadataBuilder;
import com.tangzc.autotable.core.config.PropertyConfig;
import com.tangzc.autotable.core.constants.DatabaseDialect;
import com.tangzc.autotable.core.converter.DatabaseTypeAndLength;
import com.tangzc.autotable.core.strategy.ColumnMetadata;
import com.tangzc.autotable.core.strategy.mysql.ParamValidChecker;
import com.tangzc.autotable.core.strategy.mysql.data.MysqlColumnMetadata;
import com.tangzc.autotable.core.strategy.mysql.data.MysqlTypeHelper;
import com.tangzc.autotable.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用于存放创建表的字段信息
 *
 * @author don
 */
@Slf4j
public class MysqlColumnMetadataBuilder extends ColumnMetadataBuilder {

    public MysqlColumnMetadataBuilder() {
        super(DatabaseDialect.MySQL);
    }

    @Override
    protected ColumnMetadata newColumnMetadata() {
        return new MysqlColumnMetadata();
    }

    @Override
    protected void customBuild(ColumnMetadata columnMetadata, Class<?> clazz, Field field, int position) {

        MysqlColumnMetadata mysqlColumnMetadata = (MysqlColumnMetadata) columnMetadata;

        // 列顺序位置
        mysqlColumnMetadata.setPosition(position);

        // 提取并设置字符集和排序规则
        extractCharsetAndCollate(field, mysqlColumnMetadata);

        /* 基础的校验逻辑 */
        ParamValidChecker.checkColumnParam(clazz, field, mysqlColumnMetadata);
    }

    @Override
    protected String getDefaultValue(DatabaseTypeAndLength typeAndLength, ColumnDefault columnDefault) {
        String defaultValue = super.getDefaultValue(typeAndLength, columnDefault);
        if (StringUtils.hasText(defaultValue)) {
            // 补偿逻辑：类型为Boolean的时候(实际数据库为bit数字类型)，兼容 true、false
            if (MysqlTypeHelper.isBoolean(typeAndLength) && !"1".equals(defaultValue) && !"0".equals(defaultValue)) {
                if (Boolean.parseBoolean(defaultValue)) {
                    defaultValue = "1";
                } else {
                    defaultValue = "0";
                }
            }
            // 补偿逻辑：需要兼容字符串的类型，前后自动添加'
            if (MysqlTypeHelper.isCharString(typeAndLength) && !defaultValue.isEmpty() && !defaultValue.startsWith("'") && !defaultValue.endsWith("'")) {
                defaultValue = "'" + defaultValue + "'";
            }
            // 补偿逻辑：时间类型，非函数的值，前后自动添加'
            if (MysqlTypeHelper.isDateTime(typeAndLength) && defaultValue.matches(StringUtils.DATETIME_REGEX) && !defaultValue.startsWith("'") && !defaultValue.endsWith("'")) {
                defaultValue = "'" + defaultValue + "'";
            }
        }
        return defaultValue;
    }

    @Override
    protected DatabaseTypeAndLength getTypeAndLength(String databaseDialect, Class<?> clazz, Field field) {
        DatabaseTypeAndLength typeAndLength = super.getTypeAndLength(databaseDialect, clazz, field);
        // 如果是枚举类型，但是没有指定枚举的可选值
        if (MysqlTypeHelper.isEnum(typeAndLength) && typeAndLength.getValues().isEmpty()) {
            // 判断字段是不是java的枚举类型，是的话，提取所有的枚举值
            Class<?> enumType = field.getType();
            if (enumType.isEnum()) {
                // 调用第三方框架获取枚举的可选值
                List<String> values = AutoTableGlobalConfig.getAutoTableOrmFrameAdapter().getEnumValues(enumType);
                if(values.isEmpty()) {
                    values = Arrays.stream(enumType.getEnumConstants()).map(Object::toString).collect(Collectors.toList());
                }
                typeAndLength.setValues(values);
            } else {
                // 直接报错
                String columnTypeName = ColumnType.class.getSimpleName();
                throw new RuntimeException(String.format("%s的value为：%s中的一种时，%s必须是枚举类型或者指定%s的values",
                        columnTypeName,
                        String.join(", ", MysqlTypeHelper.ENUM_OR_SET_TYPE),
                        clazz.getSimpleName() + "." + field.getName(),
                        columnTypeName
                ));
            }
        }
        return typeAndLength;
    }

    private static void extractCharsetAndCollate(Field field, MysqlColumnMetadata mysqlColumnMetadata) {

        String charset = null;
        String collate = null;
        MysqlColumnCharset mysqlColumnCharsetAnno = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, MysqlColumnCharset.class);
        if (mysqlColumnCharsetAnno != null) {
            charset = mysqlColumnCharsetAnno.value();
            if (StringUtils.hasText(mysqlColumnCharsetAnno.collate())) {
                collate = mysqlColumnCharsetAnno.collate();
            }
        } else {
            // 字符类型的添加默认的字符集和排序规则
            DatabaseTypeAndLength type = mysqlColumnMetadata.getType();
            if (MysqlTypeHelper.isCharString(type)) {
                PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();
                charset = autoTableProperties.getMysql().getColumnDefaultCharset();
                collate = autoTableProperties.getMysql().getColumnDefaultCollation();
            }
        }

        if (StringUtils.hasText(charset) && StringUtils.hasText(collate)) {
            // 字符集
            mysqlColumnMetadata.setCharacterSet(charset);
            // 字符排序
            mysqlColumnMetadata.setCollate(collate);
        }
    }
}
