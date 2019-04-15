package com.gupaoedu.mvcframework.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPAutowired {

    String value() default "";

}
