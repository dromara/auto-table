package org.dromara.autotable.core.initdata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InitDataValue {

    Class<? extends InitDataValueConverter> value() default InitDataValueConverter.DefaultInitDataValueConverter.class;
}
