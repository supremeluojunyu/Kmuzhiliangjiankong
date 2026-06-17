package com.uqm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.ImportResultVo;
import com.uqm.entity.College;
import com.uqm.entity.User;
import com.uqm.entity.UserGroupEntity;
import com.uqm.mapper.CollegeMapper;
import com.uqm.mapper.GroupMapper;
import com.uqm.mapper.UserGroupRelationMapper;
import com.uqm.mapper.UserMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.ExcelImportHelper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserImportService {

    private final UserMapper userMapper;
    private final CollegeMapper collegeMapper;
    private final GroupMapper groupMapper;
    private final UserGroupRelationMapper userGroupRelationMapper;
    private final PermissionService permissionService;
    private final DataScopeService dataScopeService;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public byte[] buildTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户导入");
            Row header = sheet.createRow(0);
            String[] cols = {"姓名*", "账号*", "密码", "学院名称*", "所属组*", "默认组", "邮箱", "企微UserId"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("张老师");
            sample.createCell(1).setCellValue("zhangsan");
            sample.createCell(2).setCellValue("admin123");
            sample.createCell(3).setCellValue("信息学院");
            sample.createCell(4).setCellValue("材料提交组,专家/评审组");
            sample.createCell(5).setCellValue("材料提交组");
            sample.createCell(6).setCellValue("zhang@school.edu");
            sample.createCell(7).setCellValue("");
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(500, "生成模板失败");
        }
    }

    @Transactional
    public ImportResultVo importUsers(LoginUser loginUser, MultipartFile file) {
        permissionService.requirePermission(loginUser, "user:manage");
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传 Excel 文件");
        }

        Integer collegeScope = dataScopeService.mutationCollegeFilter(loginUser);
        Map<String, College> collegeByName = new HashMap<>();
        for (College c : collegeMapper.selectList(null)) {
            collegeByName.put(c.getCollegeName(), c);
        }
        Map<String, UserGroupEntity> groupByName = groupMapper.selectList(null).stream()
                .collect(Collectors.toMap(UserGroupEntity::getGroupName, g -> g, (a, b) -> a));

        List<String> messages = new ArrayList<>();
        int success = 0;
        int skip = 0;
        int fail = 0;

        try {
            List<Map<String, String>> rows = ExcelImportHelper.readSheetRows(file.getInputStream());
            if (rows.isEmpty()) {
                throw new BusinessException(400, "文件中没有可导入的数据");
            }
            for (int i = 0; i < rows.size(); i++) {
                int rowNo = i + 2;
                Map<String, String> row = rows.get(i);
                try {
                    String result = importOneRow(row, collegeScope, collegeByName, groupByName);
                    if ("skip".equals(result)) {
                        skip++;
                        messages.add("第" + rowNo + "行：账号已存在，已跳过");
                    } else {
                        success++;
                    }
                } catch (Exception e) {
                    fail++;
                    messages.add("第" + rowNo + "行：" + e.getMessage());
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "解析 Excel 失败：" + e.getMessage());
        }

        operationLogService.log(loginUser, "user:import", "user", null,
                Map.of("success", success, "skip", skip, "fail", fail));

        return ImportResultVo.builder()
                .successCount(success)
                .skipCount(skip)
                .failCount(fail)
                .messages(messages)
                .build();
    }

    private String importOneRow(Map<String, String> row,
                                Integer collegeScope,
                                Map<String, College> collegeByName,
                                Map<String, UserGroupEntity> groupByName) {
        String name = ExcelImportHelper.cell(row, "姓名");
        String account = ExcelImportHelper.cell(row, "账号");
        String password = ExcelImportHelper.cell(row, "密码");
        String collegeName = ExcelImportHelper.cell(row, "学院名称");
        String groupNamesRaw = ExcelImportHelper.cell(row, "所属组");
        String defaultGroupName = ExcelImportHelper.cell(row, "默认组");
        String email = ExcelImportHelper.cell(row, "邮箱");
        String wechatUserId = ExcelImportHelper.cell(row, "企微UserId", "企微 UserId");

        if (!StringUtils.hasText(name) || !StringUtils.hasText(account) || !StringUtils.hasText(collegeName)
                || !StringUtils.hasText(groupNamesRaw)) {
            throw new BusinessException(400, "姓名、账号、学院名称、所属组不能为空");
        }

        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getAccount, account));
        if (exists > 0) {
            return "skip";
        }

        College college = collegeByName.get(collegeName);
        if (college == null) {
            throw new BusinessException(400, "学院不存在：" + collegeName);
        }
        if (collegeScope != null && !collegeScope.equals(college.getCollegeId())) {
            throw new BusinessException(403, "无权导入其他学院用户");
        }

        List<Integer> groupIds = new ArrayList<>();
        for (String part : splitList(groupNamesRaw)) {
            UserGroupEntity group = groupByName.get(part);
            if (group == null) {
                throw new BusinessException(400, "组不存在：" + part);
            }
            groupIds.add(group.getGroupId());
        }
        if (groupIds.isEmpty()) {
            throw new BusinessException(400, "至少填写一个所属组");
        }

        Integer defaultGroupId = groupIds.get(0);
        if (StringUtils.hasText(defaultGroupName)) {
            UserGroupEntity dg = groupByName.get(defaultGroupName);
            if (dg == null) {
                throw new BusinessException(400, "默认组不存在：" + defaultGroupName);
            }
            if (!groupIds.contains(dg.getGroupId())) {
                throw new BusinessException(400, "默认组必须在所属组列表中");
            }
            defaultGroupId = dg.getGroupId();
        }

        User user = new User();
        user.setName(name);
        user.setAccount(account);
        user.setCollegeId(college.getCollegeId());
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(password) ? password : "admin123"));
        user.setStatus(1);
        user.setEmail(StringUtils.hasText(email) ? email : null);
        user.setWechatUserId(StringUtils.hasText(wechatUserId) ? wechatUserId : null);
        userMapper.insert(user);
        userGroupRelationMapper.batchInsert(user.getUserId(), groupIds, defaultGroupId);
        return "ok";
    }

    private List<String> splitList(String raw) {
        return Arrays.stream(raw.split("[,，;；|]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
