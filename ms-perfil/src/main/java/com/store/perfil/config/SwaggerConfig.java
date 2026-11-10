package com.store.perfil.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Microservicio de Perfiles")
                        .version("1.0.0")
                        .description("Servicio encargado de administrar la información de los usuarios y perfiles.")
                        .contact(new Contact()
                                .name("Soporte Backend")
                                .email("soporte@store.com")));
    }

}
