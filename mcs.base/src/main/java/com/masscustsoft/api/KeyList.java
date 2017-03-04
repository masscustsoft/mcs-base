package com.masscustsoft.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * --en Field Annotation, mark a field with validation of a list of Key
 * --zh_cn 字段标注，有效性设定，允许多个键字用空格分隔
 * 
 * @author JSong
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.FIELD) 
@Documented
public @interface KeyList{
}
