package com.masscustsoft.api;

import java.lang.annotation.RetentionPolicy; 
import java.lang.annotation.Retention;
import java.lang.annotation.Target; 
import java.lang.annotation.ElementType; 
import java.lang.annotation.Documented; 

@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE) 
@Documented
public @interface SQLTable {
	String value() default "";
	//boolean supportFullText() default true;
	//boolean uniqueTableName() default false;
}
