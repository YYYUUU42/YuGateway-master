package com.yu.gateway.common.exception;


import com.yu.gateway.common.enums.ResponseCode;

/**
 * @author yu
 * @description 未找到异常
 * @date 2024-03-31
 */
public class NotFoundException extends BaseException {

	private static final long serialVersionUID = -5534700534739261761L;

	public NotFoundException(ResponseCode code) {
		super(code.getMessage(), code);
	}
	
	public NotFoundException(Throwable cause, ResponseCode code) {
		super(code.getMessage(), cause, code);
	}
	
}
