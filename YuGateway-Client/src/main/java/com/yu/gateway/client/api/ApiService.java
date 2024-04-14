package com.yu.gateway.client.api;

import java.lang.annotation.*;

/**
 * @author yu
 * @description 服务信息注解，用在服务提供类上
 * @date 2024-04-14
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiService {

    String serviceId();

    String version() default "1.0.0";

    ApiProtocol protocol();

    String patternPath();

    String interfaceName() default "";
}
