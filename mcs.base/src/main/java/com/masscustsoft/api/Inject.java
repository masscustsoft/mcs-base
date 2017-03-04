package com.masscustsoft.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.FIELD,ElementType.TYPE}) 
@Documented
public @interface Inject{
	boolean share() default false; //one instance
	String id() default ""; //shared id
	String ref() default ""; //external bean for property injection
	Class clazz() default java.lang.Object.class;
	String[] p() default {}; //@Inject(p={"model=TeFlags","lookupId=flagId","lookupTitle=flagTitle"})
	Inject1[] list() default {};
}
