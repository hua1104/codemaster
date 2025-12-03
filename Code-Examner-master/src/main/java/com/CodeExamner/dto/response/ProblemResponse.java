// dto/response/ProblemResponse.java
package com.CodeExamner.dto.response;

import com.CodeExamner.entity.enums.Difficulty;
import com.CodeExamner.entity.enums.ProblemType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProblemResponse {
    private Long id;
    private String title;
    private String description;
    private String templateCode;
    private ProblemType type;
    private String options;
    private String answer;
    private Difficulty difficulty;
    private Integer timeLimit;
    private Integer memoryLimit;
    private String creatorName;
    private LocalDateTime createTime;
    private Boolean isPublic;
}
