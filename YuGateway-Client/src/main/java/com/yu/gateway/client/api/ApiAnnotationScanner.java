package com.yu.gateway.client.api;

import com.yu.gateway.common.config.HttpServiceInvoker;
import com.yu.gateway.common.config.ServiceDefinition;
import com.yu.gateway.common.config.ServiceInvoker;
import com.yu.gateway.common.constant.BasicConst;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author yu
 * @description 注解扫描类，扫描 ApiService、ApiInvoker 注解
 * @date 2024-04-14
 */
public class ApiAnnotationScanner {

    private ApiAnnotationScanner() {
    }

    private static class SingletonHolder {
        static final ApiAnnotationScanner INSTANCE = new ApiAnnotationScanner();
    }

    public static ApiAnnotationScanner getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 扫描传入的 bean 对象，最终返回一个服务定义
     *
     * @param bean  被扫描的类
     * @param args  参数信息
     */
    public ServiceDefinition scanner(Object bean, Object... args) {
        //判断是否存在我们的暴露服务注解
        Class<?> aClass = bean.getClass();
        if (!aClass.isAnnotationPresent(ApiService.class)) {
            return null;
        }

        ApiService apiService = aClass.getAnnotation(ApiService.class);
        String serviceId = apiService.serviceId();
        ApiProtocol protocol = apiService.protocol();
        String patternPath = apiService.patternPath();
        String version = apiService.version();

        ServiceDefinition serviceDefinition = new ServiceDefinition();
        //创建一个请求路径与执行器容器
        Map<String, ServiceInvoker> invokerMap = new HashMap<>();
        //获取当前类的所有方法
        Method[] methods = aClass.getMethods();
        if (methods.length > 0) {
            for (Method method : methods) {
                ApiInvoker apiInvoker = method.getAnnotation(ApiInvoker.class);
                if (apiInvoker == null) {
                    continue;
                }

                String path = apiInvoker.path();

				if (Objects.requireNonNull(protocol) == ApiProtocol.HTTP) {
					HttpServiceInvoker httpServiceInvoker = createHttpServiceInvoker(path);
					invokerMap.put(path, httpServiceInvoker);
				}
            }

            serviceDefinition.setUniqueId(serviceId + BasicConst.COLON_SEPARATOR + version);
            serviceDefinition.setServiceId(serviceId);
            serviceDefinition.setVersion(version);
            serviceDefinition.setProtocol(protocol.getProtocol());
            serviceDefinition.setPatternPath(patternPath);
            serviceDefinition.setAvailable(true);
            serviceDefinition.setInvokerMap(invokerMap);

            return serviceDefinition;
        }

        return null;
    }


    /**
     * 构建HttpServiceInvoker对象
     */
    private HttpServiceInvoker createHttpServiceInvoker(String path) {
        HttpServiceInvoker httpServiceInvoker = new HttpServiceInvoker();
        httpServiceInvoker.setInvokerPath(path);
        return httpServiceInvoker;
    }
}