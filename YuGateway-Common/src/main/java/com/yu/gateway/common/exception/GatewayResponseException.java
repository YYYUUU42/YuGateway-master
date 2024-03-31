
package com.yu.gateway.common.exception;


import com.yu.gateway.common.enums.ResponseCode;

/**
 * @author yu
 * @description 网关响应异常
 * @date 2024-03-31
 */
public class GatewayResponseException extends GatewayBaseException {

    private static final long serialVersionUID = -5658789202509039759L;

    public GatewayResponseException() {
        this(ResponseCode.INTERNAL_ERROR);
    }

    public GatewayResponseException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public GatewayResponseException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
        this.code = code;
    }

}
