package org.zain.DataStorageUtil.XmlUtility.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD) // Field Annotation
@Retention(RetentionPolicy.RUNTIME) // Annotation available at runtime
public @interface ParentObjectIdentifier {
}
