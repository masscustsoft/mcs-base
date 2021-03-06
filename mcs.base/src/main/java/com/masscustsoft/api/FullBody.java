package com.masscustsoft.api;

import java.lang.annotation.RetentionPolicy; 
import java.lang.annotation.Retention;
import java.lang.annotation.Target; 
import java.lang.annotation.ElementType; 
import java.lang.annotation.Documented; 

@Retention(RetentionPolicy.RUNTIME)
@Target(value=ElementType.FIELD) 
@Documented
public @interface FullBody {
}
