package com.yu.gateway.core.filter.gray;

import com.yu.gateway.common.constant.FilterConst;
import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.filter.Filter;
import com.yu.gateway.core.filter.FilterAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import static com.yu.gateway.common.constant.FilterConst.GRAY_FILTER_KEY;

/**
 * @author yu
 * @description 灰度发布过滤器
 * @date 2024-04-01
 */
@FilterAspect(id = FilterConst.GRAY_FILTER_ID, name = FilterConst.GRAY_FILTER_NAME, order = FilterConst.GRAY_FILTER_ORDER)
public class GrayFilter implements Filter {
    private final Logger logger = LoggerFactory.getLogger(GrayFilter.class);

    private static String GRAY = "true";

    private static final int HASH_LENGTH = 1024;

    @Override
    public void doFilter(GatewayContext ctx) {
        if (ctx == null || ctx.getRequest() == null) {
            // 如果ctx或ctx.getRequest()为null，则直接返回，避免NullPointerException
            return;
        }

        String gray = ctx.getRequest().getHeaders().get(GRAY_FILTER_KEY);

        // 测试灰度功能待时候使用，可以手动指定其是否为灰度流量
        if (StringUtils.hasText(gray) && gray.equalsIgnoreCase(GRAY)) {
            logger.info("current user {} is set for grayService", ctx.getRequest().getClientIp());
            ctx.setGray(true);
            return;
        }

        // 选取部分灰度发布用户处理灰度流量
        String clientIp = ctx.getRequest().getClientIp();
        if (isValidClientIp(clientIp)) {
            // 使用取模运算改善边界条件
            int res = clientIp.hashCode() % HASH_LENGTH;
            if (res == 1) {
                logger.info("current client {} is selected for grayService", clientIp);
                ctx.setGray(true);
            }
        }
    }

    /**
     * 验证clientIp是否是有效的
     */
    private boolean isValidClientIp(String clientIp) {
        // 正则表达式，用于验证IPv4地址的格式
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        // 正则表达式，用于验证IPv6地址的格式
        String ipv6Pattern = "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";

        // 如果clientIp为null或者为空，那么就返回false
        if (clientIp == null || clientIp.isEmpty()) {
            return false;
        }

        // 如果clientIp匹配IPv4或IPv6的正则表达式，那么就返回true，否则返回false
        return clientIp.matches(ipv4Pattern) || clientIp.matches(ipv6Pattern);
    }
}
