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

package org.laokou.common.xxl.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.laokou.common.xxl.job.config.XxlJobProperties.PREFIX;

/**
 * @author laokou
 */
@Data
@Component
@ConfigurationProperties(prefix = PREFIX)
public class XxlJobProperties {

	public static final String PREFIX = "spring.xxl-job";

	private Admin admin;

	private Executor executor;

	@Data
	public static class Admin {

		private String address;

	}

	@Data
	public static class Executor {

		private String appName;

		private String ip;

		private Integer port;

		private String accessToken;

		private String logPath;

		private Integer intentionalities;

	}

}
