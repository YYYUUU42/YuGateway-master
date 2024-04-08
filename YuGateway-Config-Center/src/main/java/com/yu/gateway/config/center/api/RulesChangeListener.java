package com.yu.gateway.config.center.api;

import com.yu.gateway.common.config.Rule;

import java.util.List;


/**
 * @author yu
 * @description 规则变更监听器
 * @date 2024-04-08
 */
public interface RulesChangeListener {

	/**
	 * 规则变更时调用此方法 对规则进行更新
	 *
	 * @param rules 新规则
	 */
	void onRulesChange(List<Rule> rules);
}
