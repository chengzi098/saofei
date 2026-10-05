package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        try {
            Map<String, Object> data = authService.login(username, password);
            return Result.ok(data);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 查询当前用户关联的公司列表
     */
    @GetMapping("/companies")
    public Result<List<Map<String, Object>>> getCompanies() {
        Long userId = TenantContext.getUserId();
        return Result.ok(authService.getUserCompanies(userId));
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody Map<String, String> body) {
        Long userId = TenantContext.getUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        try {
            authService.changePassword(userId, oldPassword, newPassword);
            return Result.ok("密码修改成功");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
