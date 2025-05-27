package com.casino.java_online_casino;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Experimental {
    String value() default "Eksperymentalne API – używaj ostrożnie!";
}
