package com.garment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.garment.mapper")
public class GarmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(GarmentApplication.class, args);
    }
}
