package com.garment.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestAuthorizationPolicyTest {

    @Test
    void supervisorCanSubmitButCannotApproveWageAdjustment() {
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "supervisor", false, true, "POST", "/api/wage-adjustment")).isTrue();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "supervisor", false, true, "POST", "/api/wage-adjustment/12/approve")).isFalse();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "supervisor", false, true, "POST", "/api/wage-adjustment/12/reject")).isFalse();
    }

    @Test
    void workerCanUsePersonalAndScanEndpointsButNotManagementEndpoints() {
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "worker", false, true, "POST", "/api/work-record")).isTrue();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "worker", false, true, "GET", "/api/work-record")).isTrue();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "worker", false, true, "GET", "/api/wage-settlement/my")).isTrue();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "worker", false, true, "GET", "/api/employee")).isFalse();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "worker", false, true, "DELETE", "/api/style/1")).isFalse();
    }

    @Test
    void bossCanUseCompanyBusinessEndpoints() {
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "boss", false, true, "POST", "/api/wage-adjustment/12/approve")).isTrue();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "boss", false, true, "DELETE", "/api/employee/9")).isTrue();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "boss", false, true, "POST", "/api/wage-settlement/confirm")).isTrue();
    }

    @Test
    void supervisorCannotManageEmployeesOrSettlements() {
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "supervisor", false, true, "PUT", "/api/employee/9/reset-password")).isFalse();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "supervisor", false, true, "POST", "/api/wage-settlement/confirm")).isFalse();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                "supervisor", false, true, "GET", "/api/dashboard/worker-monthly-income")).isTrue();
    }

    @Test
    void platformSuperAdminAndPenetratingSuperAdminKeepSeparateScopes() {
        assertThat(RequestAuthorizationPolicy.isAllowed(
                null, true, false, "GET", "/api/company/list")).isTrue();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                null, true, false, "GET", "/api/style")).isFalse();
        assertThat(RequestAuthorizationPolicy.isAllowed(
                null, true, true, "GET", "/api/style")).isTrue();
    }
}
