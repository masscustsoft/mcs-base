package com.masscustsoft.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.FIELD,ElementType.TYPE}) 
@Documented
public @interface Inject9{
	boolean share() default false;
	String id() default "";
	String ref() default "";
	String attr() default "";
	Class clazz() default java.lang.Object.class;
	String[] p() default{};
}
