package com.garment.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("notification_read")
public class NotificationRead {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long notificationId;
    private Long userId;
    private LocalDateTime readTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getReadTime() { return readTime; }
    public void setReadTime(LocalDateTime readTime) { this.readTime = readTime; }
}
