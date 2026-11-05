package com.example.eduhub.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 문서 정보 (NFR-04). */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI eduhubOpenApi() {
    return new OpenAPI()
        .info(new Info().title("EduHub API").description("사내 교육과정 신청시스템 API").version("v1"));
  }
}
