package com.CodeExamner.dto.response;

import lombok.Data;

@Data
public class TeacherDashboardResponse {
    private Long totalExamsCreated;
    private Long ongoingExams;
    private Long scheduledExams;
    private Long finishedExams;

    private Long totalProblemsCreated;
    private Long totalSubmissionsReceived;

    private Long distinctStudentsCount;
    private Long classCount;
}

