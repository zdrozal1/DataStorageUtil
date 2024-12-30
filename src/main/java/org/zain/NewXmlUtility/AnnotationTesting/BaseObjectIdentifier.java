package org.zain.NewXmlUtility.AnnotationTesting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD) // This indicates that the annotation will be used on fields.
@Retention(RetentionPolicy.RUNTIME) // The annotation will be available at runtime.
public @interface BaseObjectIdentifier {
}
