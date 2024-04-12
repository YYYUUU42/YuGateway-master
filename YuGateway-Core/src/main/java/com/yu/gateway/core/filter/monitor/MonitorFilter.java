package com.yu.gateway.core.filter.monitor;

import com.yu.gateway.core.context.GatewayContext;
import com.yu.gateway.core.filter.Filter;
import com.yu.gateway.core.filter.FilterAspect;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import static com.yu.gateway.common.constant.FilterConst.*;

/**
 * @author yu
 * @description Prometheus入口过滤器
 * @date 2024-04-11
 */
@Slf4j
@FilterAspect(id=MONITOR_FILTER_ID, name = MONITOR_FILTER_NAME, order = MONITOR_FILTER_ORDER)
public class MonitorFilter implements Filter {

    /**
     * 执行过滤器
     */
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        ctx.setTimerSample(Timer.start());
    }
}
