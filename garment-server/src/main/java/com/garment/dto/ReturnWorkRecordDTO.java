package com.garment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 退回报工DTO
 */
public class ReturnWorkRecordDTO {

    @NotBlank(message = "退回原因不能为空")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
