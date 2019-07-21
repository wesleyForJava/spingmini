package com.wesley.annotation;

import java.lang.annotation.*;


/**
 *
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WesRequestParam{

    String value() default "";

    String name() default "";



}
