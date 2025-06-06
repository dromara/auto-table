package org.dromara.autotable.core.support.springdoc;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.media.Schema;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.core.utils.StringUtils;
import org.springdoc.core.customizers.PropertyCustomizer;

import java.lang.annotation.Annotation;
import java.util.Arrays;

public class AutoTablePropertyCustomizer implements PropertyCustomizer {
    @Override
    public Schema<?> customize(Schema property, AnnotatedType annotatedType) {
        if (property == null || annotatedType == null) {
            return property;
        }
        if (StringUtils.hasText(property.getDescription())) {
            return property;
        }
        Annotation[] annotations = annotatedType.getCtxAnnotations();
        if (annotations == null || annotations.length < 1) {
            return property;
        }
        ColumnComment columnComment = Arrays.stream(annotations)
                .filter(it -> it instanceof ColumnComment)
                .map(it -> (ColumnComment) it)
                .findAny()
                .orElse(null);
        if (columnComment != null && StringUtils.hasText(columnComment.value())) {
            return property.description(columnComment.value());
        }
        AutoColumn autoColumn = Arrays.stream(annotations)
                .filter(it -> it instanceof AutoColumn)
                .map(it -> (AutoColumn) it)
                .findAny()
                .orElse(null);
        if (autoColumn != null && StringUtils.hasText(autoColumn.comment())) {
            return property.description(autoColumn.comment());
        }
        return property;
    }
}
