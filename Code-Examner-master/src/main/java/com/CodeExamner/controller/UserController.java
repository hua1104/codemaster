// controller/UserController.java
package com.CodeExamner.controller;

import com.CodeExamner.dto.request.ChangePasswordRequest;
import com.CodeExamner.dto.request.UserProfileUpdateRequest;
import com.CodeExamner.dto.response.UserProfileResponse;
import com.CodeExamner.entity.Student;
import com.CodeExamner.entity.User;
import com.CodeExamner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的个人信息，前端对应 endpoints.auth.profile = /users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;

        if (!(principal instanceof User)) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) principal;
        return ResponseEntity.ok(toProfileResponse(user));
    }

    /**
     * 更新当前登录用户的基础信息（目前支持学生修改姓名 / 学号 / 班级）
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @RequestBody UserProfileUpdateRequest request) {
        User updated = userService.updateCurrentUserProfile(request);
        return ResponseEntity.ok(toProfileResponse(updated));
    }

    /**
     * 修改当前登录用户密码
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setRole(user.getRole());

        if (user instanceof Student) {
            Student student = (Student) user;
            profile.setStudentId(student.getStudentId());
            profile.setRealName(student.getRealName());
            profile.setClassName(student.getClassName());
        }

        return profile;
    }
}



