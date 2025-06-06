package org.dromara.autotable.core.support.springdoc;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.core.utils.StringUtils;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.core.MethodParameter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoTableParameterCustomizer implements ParameterCustomizer {


    @Override
    public Parameter customize(Parameter parameter, MethodParameter methodParameter) {
        if (parameter == null || methodParameter == null) {
            return parameter;
        }
        Annotation[] annotations = methodParameter.getParameterAnnotations();
        if (annotations.length < 1) {
            return parameter;
        }
        ColumnComment columnComment = Arrays.stream(annotations)
                .filter(it -> it instanceof ColumnComment)
                .map(it -> (ColumnComment) it)
                .findAny()
                .orElse(null);
        if (columnComment != null && StringUtils.hasText(columnComment.value())) {
            return parameter.description(columnComment.value());
        }
        AutoColumn autoColumn = Arrays.stream(annotations)
                .filter(it -> it instanceof AutoColumn)
                .map(it -> (AutoColumn) it)
                .findAny()
                .orElse(null);
        if (autoColumn != null && StringUtils.hasText(autoColumn.comment())) {
            return parameter.description(autoColumn.comment());
        }
        return parameter;
    }
}
