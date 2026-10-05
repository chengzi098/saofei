package com.garment.vo;

import java.util.List;

/**
 * 批量添加员工结果
 */
public class BatchAddResultVO {

    /** 成功添加数量 */
    private int successCount;

    /** 失败列表 */
    private List<FailItem> failures;

    public static class FailItem {
        private String phone;
        private String realName;
        private String reason;

        public FailItem() {}

        public FailItem(String phone, String realName, String reason) {
            this.phone = phone;
            this.realName = realName;
            this.reason = reason;
        }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public List<FailItem> getFailures() { return failures; }
    public void setFailures(List<FailItem> failures) { this.failures = failures; }
}
