package com.garment.vo;

import java.time.LocalDateTime;

public class NotificationVO {

    private Long id;
    private String title;
    private String content;
    private Integer targetType;
    private String senderName;
    private LocalDateTime createTime;
    private boolean read;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getTargetType() { return targetType; }
    public void setTargetType(Integer targetType) { this.targetType = targetType; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
