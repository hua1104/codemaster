// controller/ExamController.java
package com.CodeExamner.controller;

import com.CodeExamner.dto.request.ExamCreateRequest;
import com.CodeExamner.dto.response.ExamProblemDetailResponse;
import com.CodeExamner.dto.response.ExamResponse;
import com.CodeExamner.entity.Exam;
import com.CodeExamner.entity.ExamProblem;
import com.CodeExamner.repository.ExamProblemRepository;
import com.CodeExamner.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private ExamProblemRepository examProblemRepository;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody ExamCreateRequest request) {
        Exam exam = new Exam();
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setDuration(request.getDuration());

        Exam created = examService.createExam(exam);
        return ResponseEntity.ok(convertToResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody ExamCreateRequest request) {
        Exam exam = new Exam();
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setDuration(request.getDuration());

        Exam updated = examService.updateExam(id, exam);
        return ResponseEntity.ok(convertToResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamResponse> getExam(@PathVariable Long id) {
        Exam exam = examService.getExamById(id);
        return ResponseEntity.ok(convertToResponse(exam));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Page<ExamResponse>> getMyExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<Exam> exams = examService.getExamsByCreator(pageable);
        return ResponseEntity.ok(exams.map(this::convertToResponse));
    }

    @GetMapping("/available")
    public ResponseEntity<Page<ExamResponse>> getAvailableExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());
        Page<Exam> exams = examService.getAvailableExams(pageable);
        return ResponseEntity.ok(exams.map(this::convertToResponse));
    }

    @GetMapping("/ongoing")
    public ResponseEntity<List<ExamResponse>> getOngoingExams() {
        List<Exam> exams = examService.getOngoingExams();
        List<ExamResponse> responses = exams.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{examId}/problems/{problemId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ExamResponse> addProblemToExam(
            @PathVariable Long examId,
            @PathVariable Long problemId,
            @RequestParam Integer score) {
        Exam exam = examService.addProblemToExam(examId, problemId, score);
        return ResponseEntity.ok(convertToResponse(exam));
    }

    @DeleteMapping("/{examId}/problems/{problemId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ExamResponse> removeProblemFromExam(
            @PathVariable Long examId,
            @PathVariable Long problemId) {
        Exam exam = examService.removeProblemFromExam(examId, problemId);
        return ResponseEntity.ok(convertToResponse(exam));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamResponse> startExam(@PathVariable Long id) {
        Exam exam = examService.startExamForStudent(id);
        return ResponseEntity.ok(convertToResponse(exam));
    }

    @GetMapping("/{id}/can-take")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Boolean> canTakeExam(@PathVariable Long id) {
        // TODO: 从安全上下文中获取真实学生 ID
        boolean canTake = examService.canStudentTakeExam(id, 1L);
        return ResponseEntity.ok(canTake);
    }

    @GetMapping("/{id}/problems")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<ExamProblemDetailResponse>> getExamProblems(@PathVariable Long id) {
        // 确认考试存在且当前用户有权查看
        examService.getExamById(id);

        List<ExamProblem> examProblems = examProblemRepository.findByExamId(id);
        List<ExamProblemDetailResponse> responses = examProblems.stream().map(ep -> {
            ExamProblemDetailResponse resp = new ExamProblemDetailResponse();
            if (ep.getProblem() != null) {
                resp.setProblemId(ep.getProblem().getId());
                resp.setTitle(ep.getProblem().getTitle());
                resp.setDescription(ep.getProblem().getDescription());
                resp.setType(ep.getProblem().getType());
                resp.setOptions(ep.getProblem().getOptions());
            }
            resp.setScore(ep.getScore());
            resp.setSequence(ep.getSequence());
            return resp;
        }).toList();

        return ResponseEntity.ok(responses);
    }

    private ExamResponse convertToResponse(Exam exam) {
        ExamResponse response = new ExamResponse();
        response.setId(exam.getId());
        response.setTitle(exam.getTitle());
        response.setDescription(exam.getDescription());
        if (exam.getCreatedBy() != null) {
            response.setCreatorName(exam.getCreatedBy().getUsername());
        }
        response.setStartTime(exam.getStartTime());
        response.setEndTime(exam.getEndTime());
        response.setStatus(exam.getStatus());
        response.setDuration(exam.getDuration());
        response.setProblemCount(exam.getProblems() != null ? exam.getProblems().size() : 0);

        if (exam.getStatus() != null && exam.getStatus().name().equals("ONGOING")) {
            LocalDateTime now = LocalDateTime.now();
            long remainingMinutes = java.time.Duration.between(now, exam.getEndTime()).toMinutes();
            response.setRemainingTime(Math.max(0, remainingMinutes));
        }

        return response;
    }
}
