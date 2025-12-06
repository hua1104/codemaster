// service/StatisticsService.java
package com.CodeExamner.service;

import com.CodeExamner.dto.response.StatisticsResponse;
import com.CodeExamner.dto.response.TeacherClassSummaryResponse;
import com.CodeExamner.dto.response.TeacherDashboardResponse;
import com.CodeExamner.dto.response.TeacherExamScoreSummaryResponse;
import com.CodeExamner.entity.enums.ExamStatus;
import com.CodeExamner.entity.enums.JudgeStatus;
import com.CodeExamner.entity.enums.UserRole;
import com.CodeExamner.repository.ExamRepository;
import com.CodeExamner.repository.ProblemRepository;
import com.CodeExamner.repository.SubmissionRepository;
import com.CodeExamner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserService userService;

    public StatisticsResponse getSystemStatistics() {
        StatisticsResponse response = new StatisticsResponse();

        // 用户统计
        response.setTotalUsers(userRepository.count());
        response.setTotalStudents(userRepository.countByRole(UserRole.STUDENT));
        response.setTotalTeachers(userRepository.countByRole(UserRole.TEACHER));

        // 题目统计
        response.setTotalProblems(problemRepository.count());
        response.setPublicProblems(problemRepository.countByIsPublicTrue());

        // 考试统计
        response.setTotalExams(examRepository.count());
        response.setOngoingExams(examRepository.countByStatus(ExamStatus.ONGOING));
        response.setScheduledExams(examRepository.countByStatus(ExamStatus.SCHEDULED));
        response.setFinishedExams(examRepository.countByStatus(ExamStatus.FINISHED));
        response.setDraftExams(examRepository.countByStatus(ExamStatus.DRAFT));
        response.setCancelledExams(examRepository.countByStatus(ExamStatus.CANCELLED));

        // 提交统计
        response.setTotalSubmissions(submissionRepository.count());
        response.setAcceptedSubmissions(submissionRepository.countByStatus(JudgeStatus.ACCEPTED));

        return response;
    }

    public Map<String, Object> getUserStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 用户提交统计
        Long totalSubmissions = submissionRepository.countByStudentId(userId);
        Long acceptedSubmissions = submissionRepository.countByStudentIdAndStatus(userId, JudgeStatus.ACCEPTED);

        stats.put("totalSubmissions", totalSubmissions);
        stats.put("acceptedSubmissions", acceptedSubmissions);
        stats.put("acceptanceRate", totalSubmissions > 0 ?
                (double) acceptedSubmissions / totalSubmissions * 100 : 0);

        // 最近一周提交次数
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        Long recentSubmissions = submissionRepository.countByStudentIdAndSubmitTimeAfter(userId, oneWeekAgo);
        stats.put("recentSubmissions", recentSubmissions);

        return stats;
    }

    public Map<String, Object> getProblemStatistics(Long problemId) {
        Map<String, Object> stats = new HashMap<>();

        Long totalSubmissions = submissionRepository.countByProblemId(problemId);
        Long acceptedSubmissions = submissionRepository.countByProblemIdAndStatus(problemId, JudgeStatus.ACCEPTED);

        stats.put("totalSubmissions", totalSubmissions);
        stats.put("acceptedSubmissions", acceptedSubmissions);
        stats.put("acceptanceRate", totalSubmissions > 0 ?
                (double) acceptedSubmissions / totalSubmissions * 100 : 0);

        return stats;
    }

    /**
     * 教师仪表盘统计（只统计当前教师自己创建的考试 / 题目）
     */
    public TeacherDashboardResponse getTeacherDashboardStats(Long teacherId) {
        TeacherDashboardResponse resp = new TeacherDashboardResponse();

        // 考试概况
        Long totalExamsCreated = examRepository.countByCreatedById(teacherId);
        Long ongoing = examRepository.countByCreatedByIdAndStatus(teacherId, ExamStatus.ONGOING);
        Long scheduled = examRepository.countByCreatedByIdAndStatus(teacherId, ExamStatus.SCHEDULED);
        Long finished = examRepository.countByCreatedByIdAndStatus(teacherId, ExamStatus.FINISHED);

        resp.setTotalExamsCreated(totalExamsCreated != null ? totalExamsCreated : 0L);
        resp.setOngoingExams(ongoing != null ? ongoing : 0L);
        resp.setScheduledExams(scheduled != null ? scheduled : 0L);
        resp.setFinishedExams(finished != null ? finished : 0L);

        // 题库概况
        Long problems = problemRepository.countByCreatedById(teacherId);
        resp.setTotalProblemsCreated(problems != null ? problems : 0L);

        // 提交 / 学生 / 班级概况
        Long submissions = submissionRepository.countByExamCreatorId(teacherId);
        Long distinctStudents = submissionRepository.countDistinctStudentsByExamCreatorId(teacherId);
        List<String> classes = submissionRepository.findDistinctClassNamesByExamCreatorId(teacherId);

        resp.setTotalSubmissionsReceived(submissions != null ? submissions : 0L);
        resp.setDistinctStudentsCount(distinctStudents != null ? distinctStudents : 0L);
        resp.setClassCount(classes != null ? (long) classes.size() : 0L);

        return resp;
    }

    /**
     * 教师视角的“班级列表”：从其考试的提交记录中，按 student.className 聚合。
     */
    public List<TeacherClassSummaryResponse> getTeacherClassSummaries(Long teacherId) {
        List<String> classNames = submissionRepository.findDistinctClassNamesByExamCreatorId(teacherId);
        List<TeacherClassSummaryResponse> result = new ArrayList<>();

        if (classNames == null) {
            return result;
        }

        for (String className : classNames) {
            TeacherClassSummaryResponse item = new TeacherClassSummaryResponse();
            item.setClassName(className);

            Long studentCount = submissionRepository
                    .countDistinctStudentsByExamCreatorIdAndClassName(teacherId, className);
            Long submissionCount = submissionRepository
                    .countSubmissionsByExamCreatorIdAndClassName(teacherId, className);
            Double avgScore = submissionRepository
                    .findAverageScoreByExamCreatorIdAndClassName(teacherId, className);

            item.setStudentCount(studentCount != null ? studentCount : 0L);
            item.setSubmissionCount(submissionCount != null ? submissionCount : 0L);
            item.setAverageScore(avgScore != null ? avgScore : 0.0);

            result.add(item);
        }

        return result;
    }

    /**
     * 教师视角的考试成绩总览：遍历该教师创建的考试，汇总提交情况。
     */
    public List<TeacherExamScoreSummaryResponse> getTeacherExamScoreSummaries(Long teacherId) {
        // 只取当前教师创建的所有考试（不分页）
        var exams = examRepository.findByCreatedById(teacherId);
        List<TeacherExamScoreSummaryResponse> result = new ArrayList<>();

        for (var exam : exams) {
            TeacherExamScoreSummaryResponse item = new TeacherExamScoreSummaryResponse();
            item.setExamId(exam.getId());
            item.setTitle(exam.getTitle());
            item.setStatus(exam.getStatus());
            item.setStartTime(exam.getStartTime());
            item.setEndTime(exam.getEndTime());

            Long submissionCount = submissionRepository.countByExamId(exam.getId());
            Long acceptedCount = submissionRepository.countByExamIdAndStatus(exam.getId(), JudgeStatus.ACCEPTED);
            Double avgScore = submissionRepository.findAverageScoreByExamId(exam.getId());

            item.setSubmissionCount(submissionCount != null ? submissionCount : 0L);
            item.setAcceptedCount(acceptedCount != null ? acceptedCount : 0L);
            item.setAverageScore(avgScore != null ? avgScore : 0.0);

            result.add(item);
        }

        return result;
    }
}
