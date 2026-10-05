package com.garment.security;

import java.util.Set;

public final class RequestAuthorizationPolicy {
    private static final Set<String> PLATFORM_COMPANY_PATHS = Set.of(
            "/api/company/list", "/api/company/renew-logs"
    );

    private RequestAuthorizationPolicy() {}

    public static boolean isAllowed(String role, boolean superAdmin, boolean penetrating,
                                    String method, String path) {
        if (path.startsWith("/api/auth/")) return true;
        if (superAdmin) {
            return penetrating || path.startsWith("/api/company")
                    || "/api/dashboard/admin-stats".equals(path)
                    || "/api/employee/batch".equals(path);
        }
        if (role == null) return false;
        return switch (role) {
            case "boss" -> !isPlatformCompanyOperation(method, path);
            case "supervisor" -> supervisorAllowed(method, path);
            case "worker" -> workerAllowed(method, path);
            default -> false;
        };
    }

    private static boolean supervisorAllowed(String method, String path) {
        if (path.startsWith("/api/company")) {
            return "GET".equals(method) && isCompanyDetailPath(path);
        }
        if (path.startsWith("/api/wage-settlement") || path.startsWith("/api/analysis")) return false;
        if (path.startsWith("/api/employee")) return "GET".equals(method);
        if (path.matches("^/api/wage-adjustment/\\d+/(approve|reject)$")) return false;
        if (path.equals("/api/dashboard/worker-monthly-income")) return "GET".equals(method);
        if (path.startsWith("/api/dashboard/admin-stats")
                || path.startsWith("/api/dashboard/boss-")
                || path.startsWith("/api/dashboard/worker-")) return false;
        return true;
    }

    private static boolean workerAllowed(String method, String path) {
        if ("GET".equals(method) && isCompanyDetailPath(path)) return true;
        if (path.equals("/api/work-record")) return "GET".equals(method) || "POST".equals(method);
        if ("GET".equals(method) && path.startsWith("/api/bundle/scan/")) return true;
        if ("GET".equals(method) && path.equals("/api/wage-adjustment/my")) return true;
        if ("GET".equals(method) && path.equals("/api/wage-settlement/my")) return true;
        if ("POST".equals(method) && path.matches("^/api/wage-settlement/\\d+/confirm-view$")) return true;
        if (path.startsWith("/api/notification/")) {
            return path.equals("/api/notification/my")
                    || path.equals("/api/notification/read-all")
                    || path.equals("/api/notification/unread-count")
                    || path.matches("^/api/notification/\\d+/read$");
        }
        if (path.startsWith("/api/dashboard/worker-")) return true;
        return "GET".equals(method) && path.equals("/api/employee-work-details");
    }

    private static boolean isPlatformCompanyOperation(String method, String path) {
        if (PLATFORM_COMPANY_PATHS.contains(path)) return true;
        if ("POST".equals(method) && "/api/company".equals(path)) return true;
        if (!"GET".equals(method) && path.startsWith("/api/company/")) return true;
        return path.matches("^/api/company/\\d+/(renew-logs|reset-password)$");
    }

    private static boolean isCompanyDetailPath(String path) {
        return path.matches("^/api/company/\\d+$");
    }
}
