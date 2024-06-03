package com.yu.gateway.core;

import com.yu.gateway.common.utils.PropertiesUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @author yu
 * @description 核心配置加载类
 * @date 2024-04-03
 */
@Slf4j
public class ConfigLoader {
    private static final String CONFIG_FILE = "gateway.properties";
    private static final String ENV_PREFIX = "GATEWAY_";
    private static final String JVM_PREFIX = "gateway.";
    private static volatile ConfigLoader instance;
    private Config config;

    private ConfigLoader() {}

    public static Config getConfig() {
        return instance.config;
    }

    /**
     * 单例模式
     */
    public static ConfigLoader getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                if (instance == null) {
                    instance = new ConfigLoader();
                }
            }
        }
        return instance;
    }

    /**
     * 配置加载，指定优先级：运行时参数——>jvm参数——>环境变量——>配置文件——>配置对象默认值
     */
    public Config load(String[] args) {
        config = new Config();

        // 配置文件
        loadConfigFile();

        // 环境变量
        loadEnv();

        // jvm参数
        loadJvmParams();

        // 运行参数
        loadRuntimeParams(args);

        return config;
    }

    /**
     * 配置文件加载方式
     */
    public void loadConfigFile() {
        InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (inputStream != null) {
            Properties properties = new Properties();

            try {
                properties.load(inputStream);
                PropertiesUtils.propertiesToObject(properties, config);
            } catch (IOException e) {
                log.warn("load config file {} error", CONFIG_FILE, e);
            } finally {
				try {
                    inputStream.close();
				} catch (IOException e) {
					log.error("close config file {} error", CONFIG_FILE, e);
				}
			}
        }
    }

    /**
     * 环境变量加载方式
     */
    public void loadEnv() {
        Map<String, String> env = System.getenv();
        Properties properties = new Properties();
        properties.putAll(env);
        PropertiesUtils.propertiesToObject(properties, config, ENV_PREFIX);
    }

    /**
     * JVM加载方式
     */
    public void loadJvmParams() {
        Properties properties = System.getProperties();
        PropertiesUtils.propertiesToObject(properties, config, JVM_PREFIX);
    }

    /**
     * 运行时参数加载方式
     */
    public void loadRuntimeParams(String[] args) {
        if (args != null && args.length > 0) {
            Properties properties = new Properties();
            for (String arg : args) {
                if (arg.startsWith("--") && arg.contains("=")) {
                    properties.put(arg.substring(2, arg.indexOf("=")), arg.substring(arg.indexOf("=")+1));
                }
            }
            PropertiesUtils.propertiesToObject(properties, config);
        }
    }
}
