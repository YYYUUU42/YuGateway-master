package com.yu.gateway.httpService.controller;

import com.yu.gateway.client.api.ApiInvoker;
import com.yu.gateway.client.api.ApiProperties;
import com.yu.gateway.client.api.ApiProtocol;
import com.yu.gateway.client.api.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
@Slf4j
public class HttpController {

	@Autowired
	private ApiProperties apiProperties;

	@ApiInvoker(path = "/http-server/ping")
	@GetMapping("/http-server/ping")
	public String ping() {
		log.info("{}", apiProperties);
		return "pong2";
	}
}