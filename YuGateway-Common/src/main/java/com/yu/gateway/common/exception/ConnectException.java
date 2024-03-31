package com.yu.gateway.common.exception;

import com.yu.gateway.common.enums.ResponseCode;
import lombok.Getter;

/**
 * @author yu
 * @description 连接异常
 * @date 2024-03-31
 */
public class ConnectException extends BaseException {

	private static final long serialVersionUID = -8503239867913964958L;

	@Getter
	private final String uniqueId;
	
	@Getter
	private final String requestUrl;
	
	public ConnectException(String uniqueId, String requestUrl) {
		this.uniqueId = uniqueId;
		this.requestUrl = requestUrl;
	}
	
	public ConnectException(Throwable cause, String uniqueId, String requestUrl, ResponseCode code) {
		super(code.getMessage(), cause, code);
		this.uniqueId = uniqueId;
		this.requestUrl = requestUrl;
	}

}
