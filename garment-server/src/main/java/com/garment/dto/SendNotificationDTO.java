package com.garment.dto;

public class SendNotificationDTO {

    private String title;
    private String content;
    private Integer targetType;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getTargetType() { return targetType; }
    public void setTargetType(Integer targetType) { this.targetType = targetType; }
}
