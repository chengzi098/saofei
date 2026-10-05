package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.SendNotificationDTO;
import com.garment.service.NotificationService;
import com.garment.vo.NotificationVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 发送通知（老板/主管）
     */
    @PostMapping("/send")
    public Result<Void> sendNotification(@RequestBody SendNotificationDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不支持此操作");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return Result.error(400, "标题不能为空");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return Result.error(400, "内容不能为空");
        }
        notificationService.sendNotification(dto);
        return Result.ok();
    }

    /**
     * 我发送的通知列表（老板/主管）
     */
    @GetMapping("/sent")
    public Result<List<NotificationVO>> listSentNotifications() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不支持此操作");
        }
        List<NotificationVO> list = notificationService.listSentNotifications();
        return Result.ok(list);
    }

    /**
     * 我收到的通知列表（员工端）
     */
    @GetMapping("/my")
    public Result<List<NotificationVO>> listMyNotifications() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不支持此操作");
        }
        List<NotificationVO> list = notificationService.listMyNotifications();
        return Result.ok(list);
    }

    /**
     * 标记单条通知已读
     */
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不支持此操作");
        }
        notificationService.markAsRead(id);
        return Result.ok();
    }

    /**
     * 全部标记已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不支持此操作");
        }
        notificationService.markAllAsRead();
        return Result.ok();
    }

    /**
     * 未读通知计数
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不支持此操作");
        }
        int count = notificationService.getUnreadCount();
        return Result.ok(Map.of("count", count));
    }

    /**
     * 删除通知（发送人删除自己发的通知）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不支持此操作");
        }
        notificationService.deleteNotification(id);
        return Result.ok();
    }

    /**
     * 获取通知已读详情（谁读了、谁没读）
     */
    @GetMapping("/{id}/read-detail")
    public Result<Map<String, Object>> getNotificationReadDetail(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不支持此操作");
        }
        Map<String, Object> detail = notificationService.getNotificationReadDetail(id);
        if (detail == null) {
            return Result.error(404, "通知不存在");
        }
        return Result.ok(detail);
    }
}
