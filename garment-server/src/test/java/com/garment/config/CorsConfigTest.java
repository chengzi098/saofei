package com.garment.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void allowsProductionHttpsDomains() {
        CorsConfiguration config = new CorsConfig().buildCorsConfiguration();

        assertThat(config.checkOrigin("https://fjsaofei.cn"))
                .isEqualTo("https://fjsaofei.cn");
        assertThat(config.checkOrigin("https://www.fjsaofei.cn"))
                .isEqualTo("https://www.fjsaofei.cn");
    }
}
