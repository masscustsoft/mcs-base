package com.masscustsoft.api;

import java.lang.annotation.RetentionPolicy; 
import java.lang.annotation.Retention;
import java.lang.annotation.Target; 
import java.lang.annotation.ElementType; 
import java.lang.annotation.Documented; 

/**
 * Annotation to Field to indicate this field is an primary key. {@link Required} is applied automatically.
 * 
 * @author JSong
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.FIELD) 
@Documented
public @interface PrimaryKey {
	boolean serverGenerated() default false;
}
