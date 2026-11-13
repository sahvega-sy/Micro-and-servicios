package cl.clstore.resenas.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación Swagger / OpenAPI.
 * UI: /swagger-ui/index.html  |  JSON: /v3/api-docs
 * Botón "Authorize": pegar el JWT (sin la palabra Bearer) obtenido en POST /api/auth/login de ms-usuarios.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "CLstore - ms-resenas", version = "1.0"),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {
}
