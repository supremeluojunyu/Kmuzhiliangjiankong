package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqm.allocation.AllocationEngine;
import com.uqm.common.BusinessException;
import com.uqm.dto.AllocateResultVo;
import com.uqm.dto.AllocateTaskRequest;
import com.uqm.dto.FlowNode;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.dto.UserPoolItem;
import com.uqm.entity.NodeRecord;
import com.uqm.entity.TaskAllocation;
import com.uqm.entity.TaskDefinition;
import com.uqm.entity.TaskInstance;
import com.uqm.mapper.TaskAllocationMapper;
import com.uqm.mapper.TaskDefinitionMapper;
import com.uqm.mapper.TaskInstanceMapper;
import com.uqm.mapper.UserGroupQueryMapper;
import com.uqm.security.LoginUser;
import com.uqm.workflow.WorkflowEngine;
import com.uqm.workflow.WorkflowRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskAllocationService {

    private static final Set<String> VALID_TYPES = Set.of("manual", "by_college", "random", "by_total");
    private static final String LOCK_PREFIX = "lock:allocate:";

    private final TaskDefinitionMapper taskMapper;
    private final TaskInstanceMapper instanceMapper;
    private final TaskAllocationMapper allocationMapper;
    private final UserGroupQueryMapper userGroupQueryMapper;
    private final AllocationEngine allocationEngine;
    private final WorkflowEngine workflowEngine;
    private final WorkflowRuntime workflowRuntime;
    private final InstanceInitService instanceInitService;
    private final PermissionService permissionService;
    private final DataScopeService dataScopeService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public AllocateResultVo allocate(LoginUser user, AllocateTaskRequest request) {
        permissionService.requirePermission(user, "task:allocate");

        if (!VALID_TYPES.contains(request.getAllocationType())) {
            throw new BusinessException(400, "无效分配类型：" + request.getAllocationType());
        }

        String lockKey = LOCK_PREFIX + request.getTaskId();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(409, "该任务正在分配中，请稍后重试");
        }

        try {
            TaskDefinition task = taskMapper.selectById(request.getTaskId());
            if (task == null) {
                throw new BusinessException(404, "任务不存在");
            }
            if (!List.of("published", "in_progress").contains(task.getStatus())) {
                throw new BusinessException(400, "仅已发布或进行中的任务可分配");
            }

            TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
            workflowEngine.validate(config);

            List<Integer> collegeIds = request.getCollegeIds();
            if (collegeIds == null || collegeIds.isEmpty()) {
                throw new BusinessException(400, "请指定学院列表");
            }
            dataScopeService.validateCollegeIds(user, collegeIds);

            List<UserPoolItem> userPool = userGroupQueryMapper.listUsersInGroup(
                    request.getTargetGroupId(), collegeIds);
            if (userPool.isEmpty()) {
                throw new BusinessException(400, "目标组在指定学院内无可用用户");
            }

            List<UserPoolItem> selected = switch (request.getAllocationType()) {
                case "manual", "by_college" -> allocationEngine.allocateManual(userPool);
                case "random" -> allocationEngine.allocateRandom(
                        userPool, collegeIds, request.getTotalInstances());
                case "by_total" -> {
                    if (request.getTotalInstances() == null || request.getTotalInstances() <= 0) {
                        throw new BusinessException(400, "按总量分配需指定 totalInstances");
                    }
                    yield allocationEngine.allocateByTotal(
                            userPool, collegeIds, request.getTotalInstances());
                }
                default -> throw new BusinessException(400, "无效分配类型");
            };

            if (selected.isEmpty()) {
                throw new BusinessException(400, "未能生成任何任务实例");
            }

            List<Integer> instanceIds = new ArrayList<>();
            for (UserPoolItem u : selected) {
                TaskInstance instance = new TaskInstance();
                instance.setTaskDefinitionId(task.getTaskId());
                instance.setTargetGroupId(request.getTargetGroupId());
                instance.setAssignedToUserId(u.getUserId());
                instance.setCollegeId(u.getCollegeId());
                instance.setStatus("in_progress");
                instance.setCreatedAt(LocalDateTime.now());
                instanceMapper.insert(instance);
                instanceInitService.initNodeRecords(instance.getId(), config);
                instanceIds.add(instance.getId());
            }

            if ("published".equals(task.getStatus())) {
                task.setStatus("in_progress");
                taskMapper.updateById(task);
            }

            TaskAllocation record = new TaskAllocation();
            record.setTaskId(task.getTaskId());
            record.setAllocationType(request.getAllocationType());
            record.setCreatedBy(user.getUserId());
            record.setCreatedAt(LocalDateTime.now());
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("targetGroupId", request.getTargetGroupId());
                params.put("collegeIds", collegeIds);
                params.put("totalInstances", request.getTotalInstances());
                params.put("createdCount", instanceIds.size());
                record.setParamsJson(objectMapper.writeValueAsString(params));
            } catch (Exception e) {
                record.setParamsJson("{}");
            }
            allocationMapper.insert(record);

            return AllocateResultVo.builder()
                    .allocationId(record.getId())
                    .taskId(task.getTaskId())
                    .allocationType(request.getAllocationType())
                    .createdCount(instanceIds.size())
                    .instanceIds(instanceIds)
                    .build();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    public long countInstancesByTask(Integer taskId) {
        return instanceMapper.selectCount(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskDefinitionId, taskId));
    }
}
