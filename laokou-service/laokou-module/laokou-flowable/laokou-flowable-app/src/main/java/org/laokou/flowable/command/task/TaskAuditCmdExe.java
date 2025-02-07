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

package org.laokou.flowable.command.task;

import com.baomidou.dynamic.datasource.annotation.DS;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.laokou.common.core.utils.MapUtil;
import org.laokou.common.i18n.common.GlobalException;
import org.laokou.common.i18n.dto.Result;
import org.laokou.flowable.dto.task.TaskAuditCmd;
import org.laokou.flowable.dto.task.clientobject.AuditCO;
import org.laokou.flowable.gatewayimpl.database.TaskMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.laokou.flowable.common.Constant.FLOWABLE;

/**
 * @author laokou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAuditCmdExe {

	private final TaskService taskService;

	private final TaskMapper taskMapper;

	@DS(FLOWABLE)
	public Result<AuditCO> execute(TaskAuditCmd cmd) {
		log.info("审批流程分布式事务 XID:{}", RootContext.getXID());
		String taskId = cmd.getTaskId();
		Map<String, Object> values = cmd.getValues();
		String instanceId = cmd.getInstanceId();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			throw new GlobalException("任务不存在");
		}
		if (DelegationState.PENDING.equals(task.getDelegationState())) {
			throw new GlobalException("非审批任务，请处理任务");
		}
		return Result.of(audit(taskId, instanceId, values));
	}

	@Transactional(rollbackFor = Exception.class)
	public AuditCO audit(String taskId, String instanceId, Map<String, Object> values) {
		if (MapUtil.isNotEmpty(values)) {
			taskService.complete(taskId, values);
		}
		else {
			taskService.complete(taskId);
		}
		return new AuditCO(taskMapper.getAssigneeByInstanceId(instanceId));
	}

}
