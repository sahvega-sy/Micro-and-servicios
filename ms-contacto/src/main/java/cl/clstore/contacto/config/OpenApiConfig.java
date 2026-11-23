package cl.clstore.contacto.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI (Swagger).
 *  - Interfaz:  http://localhost:8088/swagger-ui.html
 *  - JSON:      http://localhost:8088/v3/api-docs
 *
 * Para probar endpoints protegidos: inicia sesión en ms-usuarios (POST /api/auth/login),
 * copia el "token" y pégalo (sin el prefijo "Bearer") en el botón "Authorize".
 * Como los 8 servicios comparten JWT_SECRET, el mismo token sirve en todos.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "ms-contacto",
        version = "1.0.0",
        description = "Mensajes del formulario de contacto de CLstore"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT emitido por ms-usuarios (POST /api/auth/login). Pega solo el token, sin 'Bearer'.")
public class OpenApiConfig {
}
