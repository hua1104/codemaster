// service/impl/SubmissionServiceImpl.java (修复版本)
package com.CodeExamner.service.impl;

import com.CodeExamner.entity.*;
import com.CodeExamner.entity.enums.ExamStatus;
import com.CodeExamner.entity.enums.JudgeStatus;
import com.CodeExamner.entity.enums.ProblemType;
import com.CodeExamner.repository.SubmissionRepository;
import com.CodeExamner.repository.ProblemRepository;
import com.CodeExamner.repository.ExamRepository;
import com.CodeExamner.service.JudgeService;
import com.CodeExamner.service.SubmissionService;
import com.CodeExamner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JudgeService judgeService;

    @Override
    public Submission submitCode(Submission submission) {
        User currentUser = userService.getCurrentUser();

        // 验证题目存在
        Problem problem = problemRepository.findById(submission.getProblem().getId())
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        submission.setStudent((Student) currentUser);
        submission.setProblem(problem);
        submission.setStatus(JudgeStatus.PENDING);

        // 如果是考试提交，验证考试状态（只禁止考试结束后的提交）
        if (submission.getExam() != null && submission.getExam().getId() != null) {
            Exam exam = examRepository.findById(submission.getExam().getId())
                    .orElseThrow(() -> new RuntimeException("考试不存在"));

            // 只要考试还没结束，就允许学生暂存 / 提交答案
            if (exam.getStatus() == ExamStatus.FINISHED || !isWithinExamTime(exam)) {
                throw new RuntimeException("不在考试时间内，无法提交答案");
            }

            submission.setExam(exam);
        }

        Submission saved = submissionRepository.save(submission);

        // 编程题：走 Judge0 评测
        if (problem.getType() == ProblemType.CODING) {
            judgeService.judgeSubmission(saved);
        } else {
            // 选择题 / 填空题：本地比对标准答案，不依赖 Judge0
            gradeNonCodingSubmission(saved, problem);
        }

        return saved;
    }

    @Override
    public Submission getSubmissionById(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        // 检查访问权限
        User currentUser = userService.getCurrentUser();
        if (!submission.getStudent().getId().equals(currentUser.getId()) &&
                !canViewAllSubmissions(currentUser)) {
            throw new RuntimeException("无权查看此提交记录");
        }

        return submission;
    }

    @Override
    public Page<Submission> getSubmissionsByUser(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return submissionRepository.findByStudentId(currentUser.getId(), pageable);
    }

    @Override
    public Page<Submission> getSubmissionsByProblem(Long problemId, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        // 学生只能看自己的提交，教师和管理员可以看所有
        if (currentUser.getRole().name().startsWith("ROLE_STUDENT")) {
            return submissionRepository.findByProblemIdAndStudentId(problemId, currentUser.getId(), pageable);
        } else {
            return submissionRepository.findByProblemId(problemId, pageable);
        }
    }

    @Override
    public Page<Submission> getSubmissionsByExam(Long examId, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        // 学生只能看自己的考试提交
        if (currentUser.getRole().name().startsWith("ROLE_STUDENT")) {
            return submissionRepository.findByExamIdAndStudentId(examId, currentUser.getId(), pageable);
        } else {
            return submissionRepository.findByExamId(examId, pageable);
        }
    }

    @Override
    public List<Submission> getUserSubmissionsInExam(Long examId, Long userId) {
        User currentUser = userService.getCurrentUser();

        // 只能查看自己的提交，或者教师查看学生的提交
        if (!currentUser.getId().equals(userId) &&
                !canViewAllSubmissions(currentUser)) {
            throw new RuntimeException("无权查看此用户的提交记录");
        }

        return submissionRepository.findByExamIdAndStudentId(examId, userId);
    }

    @Override
    public void updateSubmissionResult(Long submissionId, JudgeStatus status,
                                       Integer score, Integer timeUsed, Integer memoryUsed) {
        Submission submission = getSubmissionById(submissionId);
        submission.setStatus(status);
        submission.setScore(score);
        submission.setTimeUsed(timeUsed);
        submission.setMemoryUsed(memoryUsed);

        submissionRepository.save(submission);
    }

    @Override
    public Submission resubmit(Long submissionId) {
        Submission original = getSubmissionById(submissionId);

        Submission newSubmission = new Submission();
        newSubmission.setProblem(original.getProblem());
        newSubmission.setExam(original.getExam());
        newSubmission.setStudent(original.getStudent());
        newSubmission.setCode(original.getCode());
        newSubmission.setLanguage(original.getLanguage());
        newSubmission.setStatus(JudgeStatus.PENDING);

        return submissionRepository.save(newSubmission);
    }

    /**
     * 对选择题 / 填空题进行本地判分，不依赖 Judge0。
     * - CHOICE：学生提交的 code 字段是所选选项的标记（如 "A"），与 Problem.answer 中的正确选项比对；
     * - FILL_BLANK：Problem.answer 中保存的是 JSON 数组，学生提交的 code 按行拆分，与每个空位逐一比对。
     */
    private void gradeNonCodingSubmission(Submission submission, Problem problem) {
        String studentCode = submission.getCode() != null ? submission.getCode().trim() : "";
        int fullScore = 100;

        // 如果是考试中的提交，拿该题在考试中的满分
        if (submission.getExam() != null && submission.getExam().getId() != null) {
            Exam exam = submission.getExam();
            examRepository.findById(exam.getId()).ifPresent(e -> {
                // 此处只负责确保 Exam 已经是持久化对象，分数由 JudgeService 汇总时再读取 ExamProblem.score
            });
        }

        boolean isCorrect = false;
        boolean canJudge = false;

        if (problem.getType() == ProblemType.CHOICE) {
            // Problem.answer 存的是正确选项的 label 数组，例如 ["A","C"]
            try {
                if (problem.getAnswer() != null && !problem.getAnswer().trim().isEmpty()) {
                    java.util.List<String> correctLabels = new java.util.ArrayList<>();
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String[] arr = mapper.readValue(problem.getAnswer(), String[].class);
                    for (String s : arr) {
                        if (s != null && !s.trim().isEmpty()) {
                            correctLabels.add(s.trim());
                        }
                    }
                    if (!correctLabels.isEmpty()) {
                        canJudge = true;
                        // 当前前端为单选：学生 code 里只会有一个 label
                        isCorrect = !studentCode.isEmpty() && correctLabels.contains(studentCode);
                    }
                }
            } catch (Exception e) {
                // 标准答案配置异常：视为暂不能判分
                canJudge = false;
            }
        } else if (problem.getType() == ProblemType.FILL_BLANK) {
            // Problem.answer 中保存的是每个空的参考答案数组
            try {
                if (problem.getAnswer() != null && !problem.getAnswer().trim().isEmpty()) {
                    java.util.List<String> expected = new java.util.ArrayList<>();
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String[] arr = mapper.readValue(problem.getAnswer(), String[].class);
                    for (String s : arr) {
                        expected.add(s != null ? s.trim() : "");
                    }

                    if (!expected.isEmpty()) {
                        // 学生答案按行拆分，对应多个空
                        String[] lines = studentCode.split("\\r?\\n");
                        java.util.List<String> actual = new java.util.ArrayList<>();
                        for (String line : lines) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty()) {
                                actual.add(trimmed);
                            }
                        }

                        canJudge = (expected.size() == actual.size());
                        if (canJudge) {
                            isCorrect = true;
                            for (int i = 0; i < expected.size(); i++) {
                                if (!expected.get(i).equals(actual.get(i))) {
                                    isCorrect = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 标准答案配置异常：视为暂不能判分
                canJudge = false;
            }
        }

        submission.setTimeUsed(0);
        submission.setMemoryUsed(0);

        if (!canJudge) {
            // 没有配置有效标准答案或配置错误：保持待评测状态，不给学生错误提示
            submission.setStatus(JudgeStatus.PENDING);
            submission.setScore(null);
        } else if (isCorrect) {
            submission.setStatus(JudgeStatus.ACCEPTED);
            // 暂时统一记为满分（如果后续需要按考试设置分值，可在这里读取 ExamProblem.score）
            submission.setScore(fullScore);
        } else {
            submission.setStatus(JudgeStatus.WRONG_ANSWER);
            submission.setScore(0);
        }

        submissionRepository.save(submission);
    }

    // 修复这个方法 - 添加参数检查
    private boolean isWithinExamTime(Exam exam) {
        if (exam == null || exam.getStartTime() == null || exam.getEndTime() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return (now.isEqual(exam.getStartTime()) || now.isAfter(exam.getStartTime())) &&
                (now.isEqual(exam.getEndTime()) || now.isBefore(exam.getEndTime()));
    }

    private boolean canViewAllSubmissions(User user) {
        return user.getRole().name().startsWith("ROLE_TEACHER") ||
                user.getRole().name().startsWith("ROLE_ADMIN");
    }
}
