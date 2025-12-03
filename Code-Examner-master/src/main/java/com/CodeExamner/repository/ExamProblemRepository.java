// repository/ExamProblemRepository.java
package com.CodeExamner.repository;

import com.CodeExamner.entity.ExamProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamProblemRepository extends JpaRepository<ExamProblem, Long> {
    Optional<ExamProblem> findByExamIdAndProblemId(Long examId, Long problemId);
    void deleteByExamIdAndProblemId(Long examId, Long problemId);

    // 获取某场考试下的所有题目
    List<ExamProblem> findByExamId(Long examId);
}
