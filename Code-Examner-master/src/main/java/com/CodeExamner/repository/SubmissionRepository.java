// repository/SubmissionRepository.java
package com.CodeExamner.repository;

import com.CodeExamner.entity.Submission;
import com.CodeExamner.entity.enums.JudgeStatus; // 需要导入JudgeStatus
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // 需要导入LocalDateTime
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // 分页查询方法
    Page<Submission> findByStudentId(Long studentId, Pageable pageable);
    Page<Submission> findByProblemId(Long problemId, Pageable pageable);
    Page<Submission> findByProblemIdAndStudentId(Long problemId, Long studentId, Pageable pageable);

    // 考试相关查询
    List<Submission> findByExamIdAndStudentId(Long examId, Long studentId);

    @Query("SELECT s FROM Submission s WHERE s.exam.id = :examId")
    Page<Submission> findByExamId(Long examId, Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.exam.id = :examId AND s.student.id = :studentId")
    Page<Submission> findByExamIdAndStudentId(Long examId, Long studentId, Pageable pageable);

    // ============== 为StatisticsService添加的统计方法 ==============

    // 按状态统计
    Long countByStatus(JudgeStatus status);

    // 按学生统计
    Long countByStudentId(Long studentId);
    Long countByStudentIdAndStatus(Long studentId, JudgeStatus status);
    Long countByStudentIdAndSubmitTimeAfter(Long studentId, LocalDateTime submitTime);

    // 按题目统计
    Long countByProblemId(Long problemId);
    Long countByProblemIdAndStatus(Long problemId, JudgeStatus status);

    // 还可以添加一些有用的统计方法
    Long countByExamId(Long examId);
    Long countByExamIdAndStatus(Long examId, JudgeStatus status);

    // 最新提交，用于管理员仪表盘
    List<Submission> findTop10ByOrderBySubmitTimeDesc();

    // ============== 为教师统计新增的方法 ==============

    // 某教师创建的考试下收到的总提交数
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.exam.createdBy.id = :teacherId")
    Long countByExamCreatorId(Long teacherId);

    // 某教师考试下参与过的去重学生数
    @Query("SELECT COUNT(DISTINCT s.student.id) FROM Submission s WHERE s.exam.createdBy.id = :teacherId")
    Long countDistinctStudentsByExamCreatorId(Long teacherId);

    // 某教师考试下涉及的去重班级名称
    @Query("""
           SELECT DISTINCT s.student.className
           FROM Submission s
           WHERE s.exam.createdBy.id = :teacherId
             AND s.student.className IS NOT NULL
           """)
    List<String> findDistinctClassNamesByExamCreatorId(Long teacherId);

    // 按教师 + 班级统计学生数 / 提交数 / 平均分
    @Query("""
           SELECT COUNT(DISTINCT s.student.id)
           FROM Submission s
           WHERE s.exam.createdBy.id = :teacherId
             AND s.student.className = :className
           """)
    Long countDistinctStudentsByExamCreatorIdAndClassName(Long teacherId, String className);

    @Query("""
           SELECT COUNT(s)
           FROM Submission s
           WHERE s.exam.createdBy.id = :teacherId
             AND s.student.className = :className
           """)
    Long countSubmissionsByExamCreatorIdAndClassName(Long teacherId, String className);

    @Query("""
           SELECT AVG(s.score)
           FROM Submission s
           WHERE s.exam.createdBy.id = :teacherId
             AND s.student.className = :className
             AND s.score IS NOT NULL
           """)
    Double findAverageScoreByExamCreatorIdAndClassName(Long teacherId, String className);

    // 单个考试的平均得分（按提交记录）
    @Query("""
           SELECT AVG(s.score)
           FROM Submission s
           WHERE s.exam.id = :examId
             AND s.score IS NOT NULL
           """)
    Double findAverageScoreByExamId(Long examId);
}
