package com.yu.gateway.common.exception;


import com.yu.gateway.common.enums.ResponseCode;

/**
 * @author yu
 * @description 路径不匹配异常
 * @date 2024-03-31
 */
public class PathNoMatchedException extends BaseException {

	private static final long serialVersionUID = -6695383751311763169L;

	
	public PathNoMatchedException() {
		this(ResponseCode.PATH_NO_MATCHED);
	}
	
	public PathNoMatchedException(ResponseCode code) {
		super(code.getMessage(), code);
	}
	
	public PathNoMatchedException(Throwable cause, ResponseCode code) {
		super(code.getMessage(), cause, code);
	}
}
