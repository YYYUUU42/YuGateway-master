package com.yu.gateway.common.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author yu
 * @description 核心请求 url 的规则匹配类
 * @date 2024-03-31
 */
@Data
public class Rule implements Comparable<Rule>, Serializable {
    /**
     * 规则ID
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则协议
     */
    private String protocol;

    /**
     * 后端服务ID
     */
    private String serviceId;

    /**
     * 请求前缀
     */
    private String prefix;

    /**
     * 路径集合
     */
    private List<String> paths;

    /**
     * 优先级
     */
    Integer order;

    /**
     * 过滤器集合
     */
    private Set<FilterConfig> filterConfigs = new HashSet<FilterConfig>();

    /**
     * 重试规则
     */
    private RetryConfig retryConfig = new RetryConfig();

    /**
     * 限流规则
     */
    private Set<FlowControlConfig> flowControlConfigs = new HashSet<>();

    /**
     * 熔断规则
     */
    private Set<HystrixConfig> hystrixConfigs = new HashSet<>();

    /**
     * 规则过滤器
     */
    @Data
	public static class FilterConfig {
        /**
         * 过滤器唯一Id
         */
        private String id;

        /**
         * 过滤器规则描述:
         * 1.负载均衡过滤器：{"load_balance": "Random"}
         * 2.限流过滤器：   {}
         * 3.认证鉴权过滤器：{"auth_path": "/backend-http-server/http-server/ping"}
         */
        private String config;

		@Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            FilterConfig config = (FilterConfig) obj;
            return id.equals(config.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
    /**
     * 重试次数
     */
    @Data
    public static class RetryConfig {
        private int times;
    }

    /**
     * 熔断规则
     */
    @Data
    public static class HystrixConfig {
        /**
         * 熔断降级路径
         */
        private String path;

        /**
         * 超时时间
         */
        private int timeoutInMilliseconds;

        /**
         * 核心线程数量
         */
        private int coreThreadSize;

        /**
         * 熔断降级响应
         */
        private String fallbackResponse;
    }

    /**
     * 限流过滤器配置
     */
    @Data
    public static class FlowControlConfig {
        /**
         * 限流类型
         */
        private String type;

        /**
         * 限流对象值
         */
        private String value;

        /**
         * 限流模式——单机/分布式
         */
        private String mode;

        /**
         * 限流算法--固定窗口/令牌桶
         */
        private String algorithm;

        /**
         * 限流规则,json字符串存储
         */
        private String config;
    }

    /**
     * 添加过滤器配置
     */
    public boolean addFilterConfig(FilterConfig config) {
        return filterConfigs.add(config);
    }

    /**
     * 根据过滤器id得到过滤器配置
     */
    public FilterConfig getFilterConfigById(String id) {
        for (FilterConfig config : filterConfigs) {
            if (config.getId().equals(id)) {
                return config;
            }
        }
        return null;
    }

    public Rule() {
        super();
    }

    public Rule(String id, String name, String protocol, int order, Set<FilterConfig> configFilters) {
        super();
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.filterConfigs = configFilters;
        this.order = order;
    }

    @Override
    public int compareTo(Rule o) {
        int orderCompare = Integer.compare(o.order, getOrder());
        if (orderCompare == 0) {
            return getId().compareTo(o.getId());
        }
        return orderCompare;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Rule rule = (Rule)obj;
        return id.equals(rule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
