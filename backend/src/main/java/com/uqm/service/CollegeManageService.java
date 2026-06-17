package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.common.DeleteConfirmSupport;
import com.uqm.dto.BatchDeleteResultVo;
import com.uqm.dto.CollegeManageVo;
import com.uqm.dto.ConfirmDeleteRequest;
import com.uqm.dto.CreateCollegeRequest;
import com.uqm.dto.UpdateCollegeRequest;
import com.uqm.entity.College;
import com.uqm.entity.User;
import com.uqm.mapper.CollegeMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollegeManageService {

    private final CollegeMapper collegeMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;

    public List<CollegeManageVo> listAll(LoginUser user) {
        permissionService.requirePermission(user, "college:manage");
        return collegeMapper.selectList(new LambdaQueryWrapper<College>()
                        .orderByAsc(College::getCollegeId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public CollegeManageVo create(LoginUser user, CreateCollegeRequest request) {
        permissionService.requirePermission(user, "college:manage");
        assertUniqueName(request.getCollegeName(), null);
        assertUniqueCode(request.getCollegeCode(), null);

        College college = new College();
        college.setCollegeName(request.getCollegeName().trim());
        college.setCollegeCode(StringUtils.hasText(request.getCollegeCode())
                ? request.getCollegeCode().trim().toUpperCase()
                : null);
        college.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        collegeMapper.insert(college);

        operationLogService.log(user, "college:create", "college", college.getCollegeId(), null);
        return toVo(college);
    }

    @Transactional
    public CollegeManageVo update(LoginUser user, Integer collegeId, UpdateCollegeRequest request) {
        permissionService.requirePermission(user, "college:manage");
        College college = requireCollege(collegeId);

        if (StringUtils.hasText(request.getCollegeName())) {
            assertUniqueName(request.getCollegeName().trim(), collegeId);
            college.setCollegeName(request.getCollegeName().trim());
        }
        if (request.getCollegeCode() != null) {
            String code = StringUtils.hasText(request.getCollegeCode())
                    ? request.getCollegeCode().trim().toUpperCase()
                    : null;
            assertUniqueCode(code, collegeId);
            college.setCollegeCode(code);
        }
        if (request.getStatus() != null) {
            college.setStatus(request.getStatus());
        }
        collegeMapper.updateById(college);

        operationLogService.log(user, "college:update", "college", collegeId, null);
        return toVo(collegeMapper.selectById(collegeId));
    }

    @Transactional
    public void delete(LoginUser user, Integer collegeId) {
        permissionService.requirePermission(user, "college:manage");
        College college = requireCollege(collegeId);
        assertDeletable(collegeId);
        collegeMapper.deleteById(collegeId);
        operationLogService.log(user, "college:delete", "college", collegeId,
                java.util.Map.of("collegeName", college.getCollegeName()));
    }

    @Transactional
    public BatchDeleteResultVo deleteBatch(LoginUser user, ConfirmDeleteRequest request) {
        DeleteConfirmSupport.validate(request.getConfirmPhrase());
        permissionService.requirePermission(user, "college:manage");

        List<String> errors = new ArrayList<>();
        int deleted = 0;
        for (Integer collegeId : request.getIds()) {
            try {
                delete(user, collegeId);
                deleted++;
            } catch (BusinessException e) {
                errors.add("学院#" + collegeId + "：" + e.getMessage());
            }
        }
        return BatchDeleteResultVo.builder().deletedCount(deleted).errors(errors).build();
    }

    private College requireCollege(Integer collegeId) {
        College college = collegeMapper.selectById(collegeId);
        if (college == null) {
            throw new BusinessException(404, "学院不存在");
        }
        return college;
    }

    private void assertUniqueName(String name, Integer excludeId) {
        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<College>()
                .eq(College::getCollegeName, name);
        if (excludeId != null) {
            wrapper.ne(College::getCollegeId, excludeId);
        }
        if (collegeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "学院名称已存在");
        }
    }

    private void assertUniqueCode(String code, Integer excludeId) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<College>()
                .eq(College::getCollegeCode, code);
        if (excludeId != null) {
            wrapper.ne(College::getCollegeId, excludeId);
        }
        if (collegeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "学院代码已存在");
        }
    }

    private void assertDeletable(Integer collegeId) {
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getCollegeId, collegeId));
        if (userCount > 0) {
            throw new BusinessException(400, "该学院下仍有 " + userCount + " 名用户，请先调整用户所属学院后再删除");
        }
    }

    private CollegeManageVo toVo(College college) {
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getCollegeId, college.getCollegeId()));
        return CollegeManageVo.builder()
                .collegeId(college.getCollegeId())
                .collegeName(college.getCollegeName())
                .collegeCode(college.getCollegeCode())
                .status(college.getStatus())
                .createdAt(college.getCreatedAt())
                .userCount(userCount)
                .deletable(userCount == 0)
                .build();
    }
}
