package com.masscustsoft.api;

import java.lang.annotation.RetentionPolicy; 
import java.lang.annotation.Retention;
import java.lang.annotation.Target; 
import java.lang.annotation.ElementType; 
import java.lang.annotation.Documented; 

/**
 * --en Field Annotation, allow field value contains a JSON strsture
 * --zh_cn 字段标注，允许字段结果为JSON结构
 * 
 * @author JSong
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.FIELD}) //,ElementType.TYPE 
@Documented
public @interface JsonField {
	String value() default ""; //rename
	boolean output() default true;
	boolean xmlBean() default false;
}
