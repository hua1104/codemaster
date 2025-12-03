// entity/Problem.java
package com.CodeExamner.entity;

import com.CodeExamner.entity.enums.Difficulty;
import com.CodeExamner.entity.enums.ProblemType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "problems")
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String templateCode; // 代码模板

    @Enumerated(EnumType.STRING)
    private ProblemType type = ProblemType.CODING; // 题目类型：编程 / 选择 / 填空

    @Column(columnDefinition = "TEXT")
    private String options; // 选择题选项（JSON 格式存储）

    @Column(columnDefinition = "TEXT")
    private String answer; // 标准答案（JSON 或文本存储）

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private Integer timeLimit; // 时间限制(ms)
    private Integer memoryLimit; // 内存限制(MB)

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createTime;
    private Boolean isPublic = false;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL)
    private List<TestCase> testCases = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
