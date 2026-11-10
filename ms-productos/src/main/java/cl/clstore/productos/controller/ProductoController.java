package cl.clstore.productos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.clstore.productos.dto.ProductoRequest;
import cl.clstore.productos.dto.ProductoResponse;
import cl.clstore.productos.service.ProductoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }


    @GetMapping
    public List<ProductoResponse> listar(
            @RequestParam(name = "categoria", required = false) String categoria,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "oferta", defaultValue = "false") boolean oferta,
            @RequestParam(name = "excluir", required = false) String excluir,
            @RequestParam(name = "orden", required = false) String orden,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return servicio.listar(categoria, q, oferta, excluir, orden, limit)
            .stream().map(ProductoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable("id") String id) {
        return ProductoResponse.desde(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return ProductoResponse.desde(servicio.crear(request));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable("id") String id, @Valid @RequestBody ProductoRequest request) {
        return ProductoResponse.desde(servicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") String id) {
        servicio.eliminar(id);
    }
}
