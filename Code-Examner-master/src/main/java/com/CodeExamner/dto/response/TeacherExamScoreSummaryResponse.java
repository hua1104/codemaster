package com.CodeExamner.dto.response;

import com.CodeExamner.entity.enums.ExamStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherExamScoreSummaryResponse {
    private Long examId;
    private String title;
    private ExamStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long submissionCount;
    private Long acceptedCount;
    private Double averageScore;
}

