package cl.clstore.productos.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path raiz;

    public WebConfig(@Value("${app.uploads.dir}") String carpeta) {
        this.raiz = Paths.get(carpeta).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = raiz.toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(ubicacion)
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)));
    }
}
