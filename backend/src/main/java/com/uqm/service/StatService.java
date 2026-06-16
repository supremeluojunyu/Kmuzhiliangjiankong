package com.uqm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqm.common.BusinessException;
import com.uqm.dto.CollegeProgressVo;
import com.uqm.dto.CollegeScoreVo;
import com.uqm.dto.CollegeStatRow;
import com.uqm.dto.FlowNode;
import com.uqm.dto.NodeProgressVo;
import com.uqm.dto.NodeStatRow;
import com.uqm.dto.ReviewItemVo;
import com.uqm.dto.ScoreRecordRow;
import com.uqm.dto.ScoreSummaryVo;
import com.uqm.dto.TaskFlowConfig;
import com.uqm.dto.TaskProgressVo;
import com.uqm.entity.TaskDefinition;
import com.uqm.mapper.StatMapper;
import com.uqm.mapper.TaskDefinitionMapper;
import com.uqm.security.LoginUser;
import com.uqm.util.ScoreConfigUtil;
import com.uqm.workflow.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatService {

    private final StatMapper statMapper;
    private final TaskDefinitionMapper taskMapper;
    private final WorkflowEngine workflowEngine;
    private final PermissionService permissionService;
    private final DataScopeService dataScopeService;
    private final ObjectMapper objectMapper;

    public TaskProgressVo getTaskProgress(LoginUser user, Integer taskId) {
        dataScopeService.requireStatAccess(user);
        Integer collegeId = dataScopeService.statCollegeFilter(user);
        TaskDefinition task = requireTask(taskId);

        long total = statMapper.countInstances(taskId, collegeId);
        long completed = statMapper.countCompletedInstances(taskId, collegeId);
        double rate = total > 0 ? Math.round(completed * 10000.0 / total) / 100.0 : 0;

        TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());
        Map<String, FlowNode> nodeMap = config.getNodes() != null
                ? config.getNodes().stream().collect(Collectors.toMap(FlowNode::getNodeId, n -> n))
                : Map.of();

        List<NodeProgressVo> nodeProgress = statMapper.nodeStats(taskId, collegeId).stream()
                .map(row -> {
                    FlowNode node = nodeMap.get(row.getNodeId());
                    long t = row.getTotal() != null ? row.getTotal() : 0;
                    long c = row.getCompleted() != null ? row.getCompleted() : 0;
                    return NodeProgressVo.builder()
                            .nodeId(row.getNodeId())
                            .nodeName(node != null ? node.getNodeName() : row.getNodeId())
                            .nodeType(node != null ? node.getNodeType() : null)
                            .completed(c)
                            .total(t)
                            .rate(t > 0 ? Math.round(c * 10000.0 / t) / 100.0 : 0)
                            .build();
                }).toList();

        List<CollegeProgressVo> collegeProgress = statMapper.collegeStats(taskId, collegeId).stream()
                .map(row -> {
                    long t = row.getTotal() != null ? row.getTotal() : 0;
                    long c = row.getCompleted() != null ? row.getCompleted() : 0;
                    return CollegeProgressVo.builder()
                            .collegeId(row.getCollegeId())
                            .collegeName(row.getCollegeName())
                            .total(t)
                            .completed(c)
                            .rate(t > 0 ? Math.round(c * 10000.0 / t) / 100.0 : 0)
                            .build();
                }).toList();

        return TaskProgressVo.builder()
                .taskId(taskId)
                .taskName(task.getTaskName())
                .totalInstances(total)
                .completedInstances(completed)
                .completionRate(rate)
                .nodeProgress(nodeProgress)
                .collegeProgress(collegeProgress)
                .scope(collegeId != null ? "college" : "all")
                .scopeCollegeName(collegeProgress.size() == 1 ? collegeProgress.get(0).getCollegeName() : null)
                .build();
    }

    public ScoreSummaryVo getScoreSummary(LoginUser user, Integer taskId, String keyword) {
        dataScopeService.requireStatAccess(user);
        Integer collegeId = dataScopeService.statCollegeFilter(user);
        return buildScoreSummary(taskId, keyword, collegeId);
    }

    private ScoreSummaryVo buildScoreSummary(Integer taskId, String keyword, Integer collegeId) {
        TaskDefinition task = requireTask(taskId);
        TaskFlowConfig config = workflowEngine.parseJson(task.getConfigJson());

        Map<String, FlowNode> scoreNodes = config.getNodes() != null
                ? config.getNodes().stream()
                .filter(n -> "score".equals(n.getNodeType()))
                .collect(Collectors.toMap(FlowNode::getNodeId, n -> n))
                : Map.of();

        List<ReviewItemVo> reviews = new ArrayList<>();
        for (ScoreRecordRow row : statMapper.scoreRecords(taskId, collegeId)) {
            if (!scoreNodes.containsKey(row.getNodeId())) {
                continue;
            }
            Map<String, Object> data = parseSubmitData(row.getSubmitData());
            if (data == null) {
                continue;
            }
            FlowNode node = scoreNodes.get(row.getNodeId());
            boolean gradeMode = ScoreConfigUtil.MODE_GRADE.equals(ScoreConfigUtil.scoreMode(node));
            if (gradeMode && !data.containsKey("grade")) {
                continue;
            }
            if (!gradeMode && !data.containsKey("score")) {
                continue;
            }
            Double score = null;
            String grade = null;
            if (gradeMode) {
                grade = String.valueOf(data.get("grade"));
            } else {
                score = toDouble(data.get("score"));
            }
            String comment = data.get("comment") != null ? String.valueOf(data.get("comment")) : "";
            if (StringUtils.hasText(keyword) && !comment.contains(keyword)) {
                continue;
            }
            reviews.add(ReviewItemVo.builder()
                    .instanceId(row.getInstanceId())
                    .userName(row.getUserName())
                    .collegeName(row.getCollegeName())
                    .score(score)
                    .grade(grade)
                    .comment(comment)
                    .nodeId(row.getNodeId())
                    .nodeName(node != null ? node.getNodeName() : row.getNodeId())
                    .build());
        }

        List<ReviewItemVo> numericReviews = reviews.stream()
                .filter(r -> r.getScore() != null)
                .toList();
        DoubleSummaryStatistics stats = numericReviews.stream()
                .mapToDouble(ReviewItemVo::getScore)
                .summaryStatistics();

        List<CollegeScoreVo> collegeScores = new ArrayList<>();
        Map<String, List<ReviewItemVo>> byCollegeName = reviews.stream()
                .collect(Collectors.groupingBy(r -> r.getCollegeName() != null ? r.getCollegeName() : "未知"));
        for (Map.Entry<String, List<ReviewItemVo>> entry : byCollegeName.entrySet()) {
            List<ReviewItemVo> numericInCollege = entry.getValue().stream()
                    .filter(r -> r.getScore() != null)
                    .toList();
            DoubleSummaryStatistics cs = numericInCollege.stream()
                    .mapToDouble(ReviewItemVo::getScore)
                    .summaryStatistics();
            collegeScores.add(CollegeScoreVo.builder()
                    .collegeName(entry.getKey())
                    .average(cs.getCount() > 0 ? Math.round(cs.getAverage() * 100.0) / 100.0 : null)
                    .max(cs.getCount() > 0 ? cs.getMax() : null)
                    .min(cs.getCount() > 0 ? cs.getMin() : null)
                    .count(cs.getCount())
                    .build());
        }

        return ScoreSummaryVo.builder()
                .average(stats.getCount() > 0 ? Math.round(stats.getAverage() * 100.0) / 100.0 : null)
                .max(stats.getCount() > 0 ? stats.getMax() : null)
                .min(stats.getCount() > 0 ? stats.getMin() : null)
                .totalScores(reviews.size())
                .byCollege(collegeScores)
                .reviews(reviews)
                .build();
    }

    public byte[] exportReviewsExcel(LoginUser user, Integer taskId, String keyword) {
        permissionService.requirePermission(user, "data:export");
        Integer collegeId = dataScopeService.resolveCollegeFilter(user);
        ScoreSummaryVo summary = buildScoreSummary(taskId, keyword, collegeId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("评语汇总");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("实例ID");
            header.createCell(1).setCellValue("学院");
            header.createCell(2).setCellValue("执行人");
            header.createCell(3).setCellValue("节点");
            header.createCell(4).setCellValue("评分/等级");
            header.createCell(5).setCellValue("评语");

            int rowIdx = 1;
            for (ReviewItemVo item : summary.getReviews()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getInstanceId() != null ? item.getInstanceId() : 0);
                row.createCell(1).setCellValue(item.getCollegeName() != null ? item.getCollegeName() : "");
                row.createCell(2).setCellValue(item.getUserName() != null ? item.getUserName() : "");
                row.createCell(3).setCellValue(item.getNodeName() != null ? item.getNodeName() : "");
                row.createCell(4).setCellValue(item.getGrade() != null
                        ? item.getGrade()
                        : (item.getScore() != null ? String.valueOf(item.getScore()) : ""));
                row.createCell(5).setCellValue(item.getComment() != null ? item.getComment() : "");
            }

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(500, "导出失败：" + e.getMessage());
        }
    }

    private TaskDefinition requireTask(Integer taskId) {
        TaskDefinition task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    private Map<String, Object> parseSubmitData(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(val));
    }
}
