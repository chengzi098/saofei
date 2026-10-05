package com.garment.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerDataScopeTest {

    @Test
    void workRecordListForcesWorkerToCurrentUser() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/garment/service/WorkRecordService.java"));

        assertThat(source).contains("if (\"worker\".equals(TenantContext.getCompanyRole()))");
        assertThat(source).contains("workerId = TenantContext.getUserId();");
        assertThat(source).contains("workerName = null;");
    }
}
