/*
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.laokou.common.openfeign.config.auto;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.laokou.common.core.utils.RequestUtil;
import org.laokou.common.idempotent.aspect.IdempotentAspect;
import org.laokou.common.idempotent.utils.IdempotentUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Map;

import static org.laokou.common.core.constant.BizConstant.AUTHORIZATION;
import static org.laokou.common.core.constant.BizConstant.TRACE_ID;

/**
 * openfeign关闭ssl {@link FeignAutoConfiguration}
 *
 * @author laokou
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class OpenFeignAutoConfig extends ErrorDecoder.Default implements RequestInterceptor {

	private final IdempotentUtils idempotentUtils;

	@Bean
	public feign.Logger.Level loggerLevel() {
		return feign.Logger.Level.FULL;
	}

	@Override
	public void apply(RequestTemplate template) {
		HttpServletRequest request = RequestUtil.getHttpServletRequest();
		template.header(TRACE_ID, request.getHeader(TRACE_ID));
		template.header(AUTHORIZATION, request.getHeader(AUTHORIZATION));

		final boolean idempotent = IdempotentUtils.isIdempotent();
		if (idempotent) {
			// 获取当前Feign客户端的接口名称
			String clientName = template.feignTarget().type().getName();
			// 获取请求的URL
			String url = template.url();
			String method = template.method();
			// 将接口名称+URL+请求方式组合成一个key
			String uniqueKey = clientName + "_" + url + "_" + method;
			Map<String, String> idMap = IdempotentUtils.getRequestId();

			// 检查是否已经为这个键生成了ID
			String idempotentKey = idMap.get(uniqueKey);
			if (idempotentKey == null) {
				// 如果没有，生成一个新的幂等性ID
				idempotentKey = idempotentUtils.getIdempotentKey();
				idMap.put(uniqueKey, idempotentKey);
			}
			template.header(IdempotentAspect.REQUEST_ID, idempotentKey);
		}
	}

	@Bean
	public Retryer retryer() {
		// 最大请求次数为5，初始间隔时间为100ms
		// 下次间隔时间1.5倍递增，重试间最大间隔时间为1s
		return new Retryer.Default();
	}

	@Override
	public Exception decode(String methodKey, Response response) {
		Exception exception = super.decode(methodKey, response);
		log.error("拦截Feign报错信息：{}", exception.getMessage());
		return exception;
	}

}
