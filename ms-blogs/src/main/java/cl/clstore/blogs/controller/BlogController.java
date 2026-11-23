package cl.clstore.blogs.controller;

import cl.clstore.blogs.dto.BlogRequest;
import cl.clstore.blogs.dto.BlogResponse;
import cl.clstore.blogs.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Blogs", description = "Artículos del blog. La lectura es pública; escribir requiere Administrador o Vendedor")
@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService servicio;

    public BlogController(BlogService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Listar artículos")
    @GetMapping
    public List<BlogResponse> listar() {
        return servicio.listar().stream().map(BlogResponse::desde).toList();
    }

    @Operation(summary = "Obtener un artículo por id")
    @GetMapping("/{id}")
    public BlogResponse obtener(@PathVariable("id") String id) {
        return BlogResponse.desde(servicio.obtener(id));
    }

    @Operation(summary = "Crear un artículo")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlogResponse crear(@Valid @RequestBody BlogRequest request) {
        return BlogResponse.desde(servicio.crear(request));
    }

    @Operation(summary = "Actualizar un artículo")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public BlogResponse actualizar(@PathVariable("id") String id, @Valid @RequestBody BlogRequest request) {
        return BlogResponse.desde(servicio.actualizar(id, request));
    }

    @Operation(summary = "Eliminar un artículo")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") String id) {
        servicio.eliminar(id);
    }
}
