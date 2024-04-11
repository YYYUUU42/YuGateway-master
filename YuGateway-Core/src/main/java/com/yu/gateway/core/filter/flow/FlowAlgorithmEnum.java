package com.yu.gateway.core.filter.flow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yu
 * @description 限流算法枚举
 * @date 2024-04-10
 */
@Getter
@RequiredArgsConstructor
public enum FlowAlgorithmEnum {
    VOTE_BUCKET_ALGORITHM(1, "vote_bucket"),
    STREAM_BUCKET_ALGORITHM(2, "stream_bucket"),
    MOVE_WINDOWS_ALGORITHM(3, "move_window"),
    FIXED_WINDOWS_ALGORITHM(4, "fixed_window");

    private final Integer code;

    private final String alg;
}