package com.roomsync.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RoomSync Meeting Room Booking API")
                        .version("v1.0.0")
                        .description("Hệ thống Đặt lịch Phòng họp Nội bộ (RoomSync) tuân thủ 3 Quy tắc Bất biến Nghiệp vụ (NO_OVERLAP, MAX_2_HOURS, BUSINESS_HOURS_ONLY), phát triển theo phương pháp Spec-Driven & TDD.")
                        .contact(new Contact()
                                .name("RoomSync Engineering Team")
                                .email("dev@roomsync.internal"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}
