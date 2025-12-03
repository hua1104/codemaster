// controller/AdminController.java
package com.CodeExamner.controller;

import com.CodeExamner.dto.request.RegisterRequest;
import com.CodeExamner.dto.response.RecentActivityResponse;
import com.CodeExamner.dto.response.StatisticsResponse;
import com.CodeExamner.entity.Student;
import com.CodeExamner.entity.Submission;
import com.CodeExamner.entity.User;
import com.CodeExamner.entity.enums.UserRole;
import com.CodeExamner.repository.SubmissionRepository;
import com.CodeExamner.service.StatisticsService;
import com.CodeExamner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getSystemStatistics() {
        StatisticsResponse statistics = statisticsService.getSystemStatistics();
        return ResponseEntity.ok(statistics);
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody RegisterRequest request) {
        // 复用注册逻辑，但不返回 token，仅返回创建的用户信息
        UserRole role = request.getRole();
        if (role == null) {
            role = UserRole.STUDENT;
        }

        User user;
        if (role == UserRole.STUDENT) {
            Student student = new Student();
            student.setStudentId(request.getStudentId());
            student.setRealName(request.getRealName());
            student.setClassName(request.getClassName());
            user = student;
        } else {
            user = new User();
        }

        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setRole(role);

        User created = userService.register(user);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
        Page<User> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    // 更新用户角色
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<User> updateUserRole(
            @PathVariable Long userId,
            @RequestParam UserRole role) {
        User updatedUser = userService.updateUserRole(userId, role);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        StatisticsResponse stats = statisticsService.getSystemStatistics();

        Map<String, Object> dashboard = Map.of(
                "systemStats", stats,
                "recentActivity", "最近活动数据",
                "systemHealth", "系统运行正常"
        );

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/recent-activities")
    public ResponseEntity<List<RecentActivityResponse>> getRecentActivities() {
        List<Submission> submissions = submissionRepository.findTop10ByOrderBySubmitTimeDesc();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<RecentActivityResponse> activities = submissions.stream().map(submission -> {
            RecentActivityResponse resp = new RecentActivityResponse();
            if (submission.getSubmitTime() != null) {
                resp.setTime(submission.getSubmitTime().format(formatter));
            } else {
                resp.setTime("");
            }
            String username = submission.getStudent() != null ? submission.getStudent().getUsername() : "未知用户";
            resp.setUser(username);

            String problemTitle = submission.getProblem() != null ? submission.getProblem().getTitle() : "未知题目";
            String action;
            if (submission.getExam() != null && submission.getExam().getTitle() != null) {
                action = "在考试《" + submission.getExam().getTitle() + "》中提交了题目《" + problemTitle + "》";
            } else {
                action = "提交了题目《" + problemTitle + "》";
            }
            resp.setAction(action);
            return resp;
        }).toList();

        return ResponseEntity.ok(activities);
    }
}
