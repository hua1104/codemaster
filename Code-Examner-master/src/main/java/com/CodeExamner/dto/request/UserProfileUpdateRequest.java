// dto/request/UserProfileUpdateRequest.java
package com.CodeExamner.dto.request;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    // 目前仅支持学生更新这些基本信息
    private String studentId;
    private String realName;
    private String className;
}

