package com.masscustsoft.api;

import java.lang.annotation.RetentionPolicy; 
import java.lang.annotation.Retention;
import java.lang.annotation.Target; 
import java.lang.annotation.ElementType; 
import java.lang.annotation.Documented; 

/**
 * Annotation to Field to indicate this field required, it will apply to Field in FormModule automatically. If {@link PrimaryKey} is set, required is applied together.
 * 
 * @author JSong
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.FIELD) 
@Documented
public @interface Required {
}
