package com.yu.gateway.common.utils;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Properties 工具类
 */
public class PropertiesUtils {

    /**
     * 将 Properties 文件内容注入到对象中
     */
    public static void propertiesToObject(final Properties properties, final Object object, String prefix) {
        // 获取对象的所有公共方法
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String methodName = method.getName();

            // 检查方法名是否以"set"开头
            if (methodName.startsWith("set")) {
                try {
                    // 获取属性名
                    String tmp = methodName.substring(4);
                    String first = methodName.substring(3, 4);
                    String key = prefix + first.toLowerCase() + tmp;

                    // 从 Properties 中获取属性值
                    String property = properties.getProperty(key);
                    if (property != null) {
                        // 获取方法的参数类型
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length > 0) {
                            // 获取第一个参数的类型名
                            String cn = parameterTypes[0].getSimpleName();
                            Object arg = null;

							// 根据参数类型解析属性值
							switch (cn) {
								case "int", "Integer" -> arg = Integer.parseInt(property);
								case "long", "Long" -> arg = Long.parseLong(property);
								case "double", "Double" -> arg = Double.parseDouble(property);
								case "boolean", "Boolean" -> arg = Boolean.parseBoolean(property);
								case "float", "Float" -> arg = Float.parseFloat(property);
								case "String" -> arg = property;
								default -> {
									// 不支持的类型则跳过
									continue;
								}
							}

                            // 调用 setter 方法设置属性值
                            method.invoke(object, arg);
                        }
                    }
                } catch (Throwable ignored) {
                    // 忽略所有异常
                }
            }
        }
    }

    
    public static void propertiesToObject(final Properties p, final Object object) {
        propertiesToObject(p, object, "");
    }
 
}
