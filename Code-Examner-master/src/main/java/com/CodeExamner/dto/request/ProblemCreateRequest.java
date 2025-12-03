// dto/request/ProblemCreateRequest.java
package com.CodeExamner.dto.request;

import com.CodeExamner.entity.enums.Difficulty;
import com.CodeExamner.entity.enums.ProblemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProblemCreateRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private String templateCode;

    @NotNull
    private Difficulty difficulty;

    // 新增：题目类型（编程 / 选择 / 填空等）
    private ProblemType type;

    // 选择题 / 填空题配置
    private String options; // 选择题选项（JSON）
    private String answer;  // 标准答案

    @NotNull
    private Integer timeLimit;

    @NotNull
    private Integer memoryLimit;

    private Boolean isPublic = false;
}
