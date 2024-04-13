package com.yu.gateway.core.filter;

import java.lang.annotation.*;

/**
 * @author yu
 * @description 作为AOP切面提供增强功能
 * @date 2024-04-01
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface FilterAspect {

	/**
	 * 过滤器 ID
	 */
	String id();

	/**
	 * 过滤器名称
	 */
	String name() default "";

	/**
	 * 排序
	 */
	int order() default 0;
}
