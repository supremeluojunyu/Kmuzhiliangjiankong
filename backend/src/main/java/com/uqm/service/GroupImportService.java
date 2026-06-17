package com.uqm.service;

import com.uqm.common.BusinessException;
import com.uqm.dto.ImportResultVo;
import com.uqm.entity.Permission;
import com.uqm.entity.UserGroupEntity;
import com.uqm.mapper.GroupMapper;
import com.uqm.mapper.GroupPermissionMapper;
import com.uqm.mapper.PermissionMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.ExcelImportHelper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupImportService {

    private final GroupMapper groupMapper;
    private final PermissionMapper permissionMapper;
    private final GroupPermissionMapper groupPermissionMapper;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;

    public byte[] buildTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("组导入");
            Row header = sheet.createRow(0);
            String[] cols = {"组名称*", "描述", "权限代码"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("2026质量监控联络组");
            sample.createCell(1).setCellValue("负责本院质量监控协调");
            sample.createCell(2).setCellValue("message:send,stat:view_college");
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
    public ImportResultVo importGroups(LoginUser loginUser, MultipartFile file) {
        permissionService.requirePermission(loginUser, "group:manage");
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传 Excel 文件");
        }

        Map<String, Permission> permByCode = permissionMapper.selectList(null).stream()
                .collect(Collectors.toMap(Permission::getPermissionCode, p -> p, (a, b) -> a));
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
                    String groupName = ExcelImportHelper.cell(row, "组名称");
                    String result = importOneRow(row, groupByName, permByCode);
                    if ("skip".equals(result)) {
                        skip++;
                        messages.add("第" + rowNo + "行：组「" + groupName + "」已存在，已跳过");
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

        operationLogService.log(loginUser, "group:import", "group", null,
                Map.of("success", success, "skip", skip, "fail", fail));

        return ImportResultVo.builder()
                .successCount(success)
                .skipCount(skip)
                .failCount(fail)
                .messages(messages)
                .build();
    }

    private String importOneRow(Map<String, String> row,
                                Map<String, UserGroupEntity> groupByName,
                                Map<String, Permission> permByCode) {
        String groupName = ExcelImportHelper.cell(row, "组名称");
        String description = ExcelImportHelper.cell(row, "描述");
        String permCodesRaw = ExcelImportHelper.cell(row, "权限代码");

        if (!StringUtils.hasText(groupName)) {
            throw new BusinessException(400, "组名称不能为空");
        }
        if (groupByName.containsKey(groupName)) {
            return "skip";
        }

        UserGroupEntity group = new UserGroupEntity();
        group.setGroupName(groupName);
        group.setDescription(StringUtils.hasText(description) ? description : null);
        groupMapper.insert(group);

        List<Integer> permIds = new ArrayList<>();
        for (String code : splitList(permCodesRaw)) {
            Permission perm = permByCode.get(code);
            if (perm == null) {
                throw new BusinessException(400, "权限代码不存在：" + code);
            }
            permIds.add(perm.getPermissionId());
        }
        if (!permIds.isEmpty()) {
            groupPermissionMapper.batchInsert(group.getGroupId(), permIds);
        }
        groupByName.put(groupName, group);
        return "ok";
    }

    private List<String> splitList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,，;；|]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
