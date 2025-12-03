// dto/response/ExamProblemDetailResponse.java
package com.CodeExamner.dto.response;

import com.CodeExamner.entity.enums.ProblemType;
import lombok.Data;

@Data
public class ExamProblemDetailResponse {
    private Long problemId;
    private String title;
    private String description;
    private Integer score;
    private Integer sequence;

    // 新增：用于学生端按题型展示
    private ProblemType type;
    private String options;
}
