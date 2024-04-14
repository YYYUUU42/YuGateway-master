package com.yu.gateway.client.api;/**
 * @Author:Serendipity
 * @Date:
 * @Description:
 */

import java.lang.annotation.*;

/**
 * @author yu
 * @description 服务调用注解，用在服务提供类的方法上
 * @date 2024-04-14
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiInvoker {
    String path();
}
