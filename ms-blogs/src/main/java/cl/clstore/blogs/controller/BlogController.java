package cl.clstore.blogs.controller;

import cl.clstore.blogs.dto.BlogRequest;
import cl.clstore.blogs.dto.BlogResponse;
import cl.clstore.blogs.service.BlogService;
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

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService servicio;

    public BlogController(BlogService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<BlogResponse> listar() {
        return servicio.listar().stream().map(BlogResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public BlogResponse obtener(@PathVariable("id") String id) {
        return BlogResponse.desde(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlogResponse crear(@Valid @RequestBody BlogRequest request) {
        return BlogResponse.desde(servicio.crear(request));
    }

    @PutMapping("/{id}")
    public BlogResponse actualizar(@PathVariable("id") String id, @Valid @RequestBody BlogRequest request) {
        return BlogResponse.desde(servicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") String id) {
        servicio.eliminar(id);
    }
}
