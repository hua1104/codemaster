// service/impl/JudgeServiceImpl.java
package com.CodeExamner.service.impl;

import com.CodeExamner.entity.Exam;
import com.CodeExamner.entity.ExamProblem;
import com.CodeExamner.entity.Submission;
import com.CodeExamner.entity.SubmissionDetail;
import com.CodeExamner.entity.TestCase;
import com.CodeExamner.entity.enums.JudgeStatus;
import com.CodeExamner.judge0.Judge0Client;
import com.CodeExamner.judge0.Judge0Submission;
import com.CodeExamner.judge0.Judge0Result;
import com.CodeExamner.repository.ExamProblemRepository;
import com.CodeExamner.repository.SubmissionRepository;
import com.CodeExamner.repository.TestCaseRepository;
import com.CodeExamner.service.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    @Autowired
    private Judge0Client judge0Client;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ExamProblemRepository examProblemRepository;

    @Override
    @Async
    public void judgeSubmission(Submission submission) {
        try {
            // 获取题目的所有测试用例
            List<TestCase> testCases = testCaseRepository.findByProblemId(submission.getProblem().getId());

            if (testCases.isEmpty()) {
                // 未配置测试用例时，不判为运行错误，而是保持待评测状态，方便教师后续补充测试用例后重新评测
                submission.setStatus(JudgeStatus.PENDING);
                submission.setScore(null);
                submissionRepository.save(submission);
                return;
            }

            // 更新提交状态为评测中
            submission.setStatus(JudgeStatus.JUDGING);
            submissionRepository.save(submission);

            // 对每个测试用例进行评测
            for (TestCase testCase : testCases) {
                Judge0Submission judge0Submission = new Judge0Submission();
                judge0Submission.setSourceCode(submission.getCode());
                // 根据提交语言映射到 Judge0 的 language_id，默认使用 Java
                judge0Submission.setLanguageId(mapLanguageToJudge0(submission.getLanguage()));
                judge0Submission.setStdin(testCase.getInput());
                judge0Submission.setExpectedOutput(testCase.getExpectedOutput());
                judge0Submission.setCpuTimeLimit(2.0); // 2秒时间限制
                judge0Submission.setMemoryLimit(128); // 128MB内存限制

                // 提交到Judge0
                Judge0Submission result = judge0Client.submitCode(judge0Submission);

                if (result != null && result.getToken() != null) {
                    // 创建提交详情记录
                    SubmissionDetail detail = new SubmissionDetail();
                    detail.setSubmission(submission);
                    detail.setTestCase(testCase);
                    detail.setStatus(JudgeStatus.JUDGING);
                    submission.getDetails().add(detail);

                    // 异步处理评测结果
                    processJudgeResultAsync(submission.getId(), result.getToken(), testCase.getId());
                }
            }

        } catch (Exception e) {
            log.error("评测提交失败: {}", e.getMessage());
            // 调用 Judge0 失败时，不把责任算在学生代码上，保持为待评测
            submission.setStatus(JudgeStatus.PENDING);
            submission.setScore(null);
            submissionRepository.save(submission);
        }
    }

    @Async
    public CompletableFuture<Void> processJudgeResultAsync(Long submissionId, String token, Long testCaseId) {
        try {
            // 等待评测完成
            Thread.sleep(2000);

            Judge0Result result = judge0Client.getSubmissionResult(token);
            if (result != null && result.getStatus() != null) {
                updateSubmissionDetail(submissionId, testCaseId, result);
            }
        } catch (Exception e) {
            log.error("处理评测结果失败: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private void updateSubmissionDetail(Long submissionId, Long testCaseId, Judge0Result result) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) return;

        SubmissionDetail detail = submission.getDetails().stream()
                .filter(d -> d.getTestCase().getId().equals(testCaseId))
                .findFirst()
                .orElse(null);

        if (detail != null) {
            // 根据Judge0状态更新评测状态
            JudgeStatus status = mapJudge0Status(result.getStatus().getId());
            detail.setStatus(status);
            detail.setOutput(result.getStdout());
            detail.setErrorMessage(result.getStderr() != null ? result.getStderr() : result.getCompileOutput());

            if (result.getTime() != null) {
                detail.setTimeUsed((int)(Double.parseDouble(result.getTime()) * 1000));
            }
            if (result.getMemory() != null) {
                detail.setMemoryUsed(result.getMemory().intValue());
            }

            // 先保存当前测试点的评测结果
            submissionRepository.save(submission);

            // 如果所有测试点都已完成评测，则汇总更新提交的最终状态与得分
            aggregateSubmissionResult(submission);
        }
    }

    @Override
    public void processJudgeResult(Long submissionId, String judge0Token) {
        // 实现同步处理评测结果
    }

    /**
     * 将 Judge0 返回的状态 ID 映射为系统内部的 JudgeStatus。
     * 参考 Judge0 文档：
     * 1: In Queue, 2: Processing, 3: Accepted, 4: Wrong Answer,
     * 5: Time Limit Exceeded, 6: Compilation Error, 7-12: Runtime Error 等。
     */
    private JudgeStatus mapJudge0Status(Integer judge0StatusId) {
        if (judge0StatusId == null) return JudgeStatus.RUNTIME_ERROR;

        switch (judge0StatusId) {
            case 1: // In Queue
            case 2: // Processing
                return JudgeStatus.JUDGING;
            case 3: // Accepted
                return JudgeStatus.ACCEPTED;
            case 4: // Wrong Answer
                return JudgeStatus.WRONG_ANSWER;
            case 5: // Time Limit Exceeded
                return JudgeStatus.TIME_LIMIT_EXCEEDED;
            case 6: // Compilation Error
                return JudgeStatus.COMPILATION_ERROR;
            case 7:  // Runtime Error (SIGSEGV)
            case 8:  // Runtime Error (SIGXFSZ)
            case 9:  // Runtime Error (SIGFPE)
            case 10: // Runtime Error (SIGABRT)
            case 11: // Runtime Error (NZEC)
            case 12: // Runtime Error (Other)
            case 13: // Internal Error
            case 14: // Exec Format Error
                return JudgeStatus.RUNTIME_ERROR;
            default:
                return JudgeStatus.RUNTIME_ERROR;
        }
    }

    /**
     * 根据提交语言映射到 Judge0 的 language_id。
     * 这里只做常用语言的简单映射，默认使用 Java。
     */
    private int mapLanguageToJudge0(String language) {
        if (language == null) {
            return 62; // Java (OpenJDK 13.0.1)
        }
        String lang = language.trim().toLowerCase();
        switch (lang) {
            case "c":
                return 50; // C (GCC 9.2.0)
            case "cpp":
            case "c++":
            case "cpp17":
                return 54; // C++ (GCC 9.2.0)
            case "python":
            case "python3":
            case "py":
                return 71; // Python (3.8.1)
            case "java":
            default:
                return 62; // Java (OpenJDK 13.0.1)
        }
    }

    /**
     * 汇总一个提交在所有测试用例上的评测结果，计算最终得分与状态。
     * 当前策略：
     * - 所有测试点都 ACCEPTED：提交状态设为 ACCEPTED，得分为该题在考试中的满分（或普通提交给 100 分）。
     * - 只要有一个测试点非 ACCEPTED：提交状态为第一个非 ACCEPTED 的状态，得分为 0。
     * - timeUsed 取所有测试点 timeUsed 之和，memoryUsed 取所有测试点 memoryUsed 的最大值。
     */
    private void aggregateSubmissionResult(Submission submission) {
        // 如果还有测试点在评测中，则先不汇总
        boolean allDone = submission.getDetails().stream()
                .allMatch(d -> d.getStatus() != JudgeStatus.PENDING && d.getStatus() != JudgeStatus.JUDGING);
        if (!allDone) {
            return;
        }

        int totalCases = submission.getDetails().size();
        long acceptedCases = submission.getDetails().stream()
                .filter(d -> d.getStatus() == JudgeStatus.ACCEPTED)
                .count();

        // 统计耗时与内存
        int totalTimeUsed = submission.getDetails().stream()
                .map(d -> d.getTimeUsed() != null ? d.getTimeUsed() : 0)
                .reduce(0, Integer::sum);
        int maxMemoryUsed = submission.getDetails().stream()
                .map(d -> d.getMemoryUsed() != null ? d.getMemoryUsed() : 0)
                .reduce(0, Math::max);

        submission.setTimeUsed(totalTimeUsed);
        submission.setMemoryUsed(maxMemoryUsed);

        // 计算满分：如果是考试中的提交，使用 ExamProblem.score，否则默认为 100
        int fullScore = 100;
        Exam exam = submission.getExam();
        if (exam != null && exam.getId() != null) {
            examProblemRepository.findByExamIdAndProblemId(exam.getId(), submission.getProblem().getId())
                    .ifPresent(ep -> submission.setScore(ep.getScore() != null ? ep.getScore() : 0));
            fullScore = submission.getScore() != null ? submission.getScore() : 0;
        }

        if (acceptedCases == totalCases) {
            // 所有测试点通过
            submission.setStatus(JudgeStatus.ACCEPTED);
            submission.setScore(fullScore);
        } else {
            // 至少有一个测试点未通过，得分记 0，状态取第一个非 ACCEPTED 的状态
            JudgeStatus firstBadStatus = submission.getDetails().stream()
                    .map(SubmissionDetail::getStatus)
                    .filter(s -> s != JudgeStatus.ACCEPTED)
                    .findFirst()
                    .orElse(JudgeStatus.RUNTIME_ERROR);
            submission.setStatus(firstBadStatus);
            submission.setScore(0);
        }

        submissionRepository.save(submission);
    }
}
