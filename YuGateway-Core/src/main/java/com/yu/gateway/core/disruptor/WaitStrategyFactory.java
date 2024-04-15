package com.yu.gateway.core.disruptor;

import com.lmax.disruptor.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yu
 * @description Disruptor 等待策略工厂
 * @date 2024-04-15
 */
public class WaitStrategyFactory {
    private static Map<String, WaitStrategy> WAITSTRATEGY_MAP = new HashMap<>();

    static {
        WAITSTRATEGY_MAP.put(WAITKEY.BLOCKING, new BlockingWaitStrategy());
        WAITSTRATEGY_MAP.put(WAITKEY.BUSYSPIN, new BusySpinWaitStrategy());
        WAITSTRATEGY_MAP.put(WAITKEY.YIELD, new YieldingWaitStrategy());
        WAITSTRATEGY_MAP.put(WAITKEY.SLEEPING, new SleepingWaitStrategy());
    }

    private static final WaitStrategy DEFAULT_WAITSTRATEGY = WAITSTRATEGY_MAP.get(WAITKEY.BLOCKING);

    private interface WAITKEY {
        String BLOCKING = "blocking";
        String BUSYSPIN = "busySpin";
        String YIELD = "yielding";
        String SLEEPING = "sleeping";
    }

    private WaitStrategyFactory() {
    }

    public static WaitStrategy getWaitStrategy(String waitKey) {
        WaitStrategy waitStrategy = WAITSTRATEGY_MAP.get(waitKey);
        return waitStrategy == null ? DEFAULT_WAITSTRATEGY : waitStrategy;
    }
}
