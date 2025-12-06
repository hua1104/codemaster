package com.CodeExamner.dto.response;

import lombok.Data;

@Data
public class TeacherClassSummaryResponse {
    private String className;
    private Long studentCount;
    private Long submissionCount;
    private Double averageScore;
}

