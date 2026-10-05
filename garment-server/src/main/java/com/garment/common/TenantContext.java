package com.garment.common;

/**
 * 租户上下文 - ThreadLocal 存储当前公司 ID
 */
public class TenantContext {
    private static final ThreadLocal<Long> COMPANY_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_SUPER_ADMIN = new ThreadLocal<>();
    private static final ThreadLocal<Integer> USER_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<String> COMPANY_ROLE = new ThreadLocal<>();

    public static void setCompanyId(Long companyId) {
        COMPANY_ID.set(companyId);
    }

    public static Long getCompanyId() {
        return COMPANY_ID.get();
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setIsSuperAdmin(boolean isSuper) {
        IS_SUPER_ADMIN.set(isSuper);
    }

    public static boolean isSuperAdmin() {
        Boolean v = IS_SUPER_ADMIN.get();
        return v != null && v;
    }

    public static void setUserType(Integer userType) {
        USER_TYPE.set(userType);
    }

    public static Integer getUserType() {
        return USER_TYPE.get();
    }

    public static void setCompanyRole(String role) {
        COMPANY_ROLE.set(role);
    }

    public static String getCompanyRole() {
        return COMPANY_ROLE.get();
    }

    /**
     * 超管是否正在穿透进入某公司（超管 + 携带了 companyId）
     */
    public static boolean isPenetrating() {
        return isSuperAdmin() && getCompanyId() != null;
    }

    public static void clear() {
        COMPANY_ID.remove();
        USER_ID.remove();
        IS_SUPER_ADMIN.remove();
        USER_TYPE.remove();
        COMPANY_ROLE.remove();
    }
}
