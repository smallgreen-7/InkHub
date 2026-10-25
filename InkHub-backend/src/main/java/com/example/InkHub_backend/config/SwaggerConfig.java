package com.example.InkHub_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 接口文档：SpringDoc 3.x（适配 Spring Boot 4），启动后访问 http://localhost:8080/swagger-ui.html
@Configuration
public class SwaggerConfig {

    // 全局 JWT 鉴权：Swagger UI 右上角会出现 Authorize 按钮
    private static final String SECURITY_SCHEME_NAME = "Authorization";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("InkHub 知识博客 API")
                        .version("1.0.0")
                        .description("文章 / 评论 / 点赞收藏 / 统计接口文档\n"
                                + "🔐 需要登录的接口：先在登录接口调试获取 token，"
                                + "点右上角 Authorize 填入（不要带 Bearer 前缀）")
                        .contact(new Contact().name("InkHub Team"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
