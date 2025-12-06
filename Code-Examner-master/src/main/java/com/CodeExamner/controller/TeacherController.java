package com.CodeExamner.controller;

import com.CodeExamner.dto.response.TeacherClassSummaryResponse;
import com.CodeExamner.dto.response.TeacherDashboardResponse;
import com.CodeExamner.dto.response.TeacherExamScoreSummaryResponse;
import com.CodeExamner.entity.User;
import com.CodeExamner.entity.enums.UserRole;
import com.CodeExamner.service.StatisticsService;
import com.CodeExamner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    @Autowired
    private UserService userService;

    @Autowired
    private StatisticsService statisticsService;

    private Long getCurrentTeacherId() {
        User current = userService.getCurrentUser();
        if (current.getRole() != UserRole.TEACHER && current.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("仅教师可访问该接口");
        }
        return current.getId();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<TeacherDashboardResponse> getDashboard() {
        Long teacherId = getCurrentTeacherId();
        TeacherDashboardResponse resp = statisticsService.getTeacherDashboardStats(teacherId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/classes")
    public ResponseEntity<List<TeacherClassSummaryResponse>> getClasses() {
        Long teacherId = getCurrentTeacherId();
        List<TeacherClassSummaryResponse> list = statisticsService.getTeacherClassSummaries(teacherId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/scores")
    public ResponseEntity<List<TeacherExamScoreSummaryResponse>> getScores() {
        Long teacherId = getCurrentTeacherId();
        List<TeacherExamScoreSummaryResponse> list = statisticsService.getTeacherExamScoreSummaries(teacherId);
        return ResponseEntity.ok(list);
    }
}

