package com.garment.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveConfigurationTest {

    @Test
    void applicationConfigRequiresSecretsAndKeepsLongLivedToken() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(config).contains("password: ${DB_PASSWORD}");
        assertThat(config).contains("secret: ${JWT_SECRET}");
        assertThat(config).contains("expiration: 3153600000000");
        assertThat(config).doesNotContain("cc2002" + "0716");
        assertThat(config).doesNotContain("garment_jwt_secret_key_" + "2024_very_long_and_secure");
    }
}
