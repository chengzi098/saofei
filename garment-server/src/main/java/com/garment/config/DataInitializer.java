package com.garment.config;

import com.garment.entity.User;
import com.garment.service.UserService;
import com.garment.util.PasswordGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时自动初始化默认超级管理员账号
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        // 检查是否已有 admin 账号
        if (userService.getByUsername("admin") == null) {
            // 超管初始密码：优先读环境变量，未设置则随机生成并打印到日志一次
            String tempPassword = System.getenv("ADMIN_INIT_PASSWORD");
            boolean isRandom = false;
            if (tempPassword == null || tempPassword.isEmpty()) {
                tempPassword = PasswordGenerator.generate(16);
                isRandom = true;
            }

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(tempPassword);
            admin.setRealName("超级管理员");
            admin.setType(0);
            admin.setNeedChangePassword(1);
            userService.createUser(admin);

            if (isRandom) {
                // 随机生成的密码只打印这一次，请立即抄录并妥善保管
                System.out.println("============================================================");
                System.out.println(">>> 超级管理员账号已创建: admin");
                System.out.println(">>> 随机初始密码（仅显示此次，请立即抄录并妥善保管）: " + tempPassword);
                System.out.println(">>> 生产环境建议设置环境变量 ADMIN_INIT_PASSWORD 以固定初始密码");
                System.out.println("============================================================");
            } else {
                System.out.println(">>> 默认超级管理员账号已创建: admin（密码来自环境变量 ADMIN_INIT_PASSWORD）");
            }
        }
    }
}
