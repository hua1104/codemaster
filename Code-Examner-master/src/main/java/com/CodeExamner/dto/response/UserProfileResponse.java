// dto/response/UserProfileResponse.java
package com.CodeExamner.dto.response;

import com.CodeExamner.entity.enums.UserRole;
import lombok.Data;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private UserRole role;

    // 学生特有字段（如果是 Student）
    private String studentId;
    private String realName;
    private String className;
}



