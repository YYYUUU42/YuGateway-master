package com.yu.gateway.core.filter.flow;

public interface FlowAlgorithmConstant {
	/**
	 * 令牌桶算法
	 */
	String VOTE_BUCKET_ALGORITHM = "vote_bucket";

	/**
	 * 漏桶算法
	 */
	String STREAM_BUCKET_ALGORITHM = "stream_bucket";

	/**
	 * 移动窗口算法
	 */
	String MOVE_WINDOWS_ALGORITHM = "move_window";

	/**
	 * 固定窗口算法
	 */
	String FIXED_WINDOWS_ALGORITHM = "fixed_window";
}
