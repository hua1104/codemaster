// config/DataInitializer.java
package com.CodeExamner.config;

import com.CodeExamner.entity.User;
import com.CodeExamner.entity.enums.UserRole;
import com.CodeExamner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 应用启动时初始化一个默认管理员账户，避免因为手动插入密码哈希不一致导致无法登录。
 *
 * 默认账号：
 *  - 用户名：admin
 *  - 密码：admin123
 */
@Configuration
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initAdminUser() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
        }

        admin.setRole(UserRole.ADMIN);
        admin.setPassword(passwordEncoder.encode("admin123"));

        userRepository.save(admin);
    }
}



