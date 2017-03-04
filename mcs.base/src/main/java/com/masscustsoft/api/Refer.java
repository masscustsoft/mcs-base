package com.masscustsoft.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.masscustsoft.api.Referent;

@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE) 
@Documented
public @interface Refer {
	Referent[] value() default {};
}
