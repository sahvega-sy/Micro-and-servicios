package cl.clstore.productos.controller;

import cl.clstore.productos.dto.ProductoRequest;
import cl.clstore.productos.dto.ProductoResponse;
import cl.clstore.productos.service.ProductoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Productos", description = "Catálogo de productos. La lectura es pública; escribir requiere Administrador o Vendedor")
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    /**
     * Listado con filtros opcionales:
     *  categoria=componentes | q=texto | oferta=true | excluir=idProducto | ids=a,b,c |
     *  orden=nombre|creado_asc|creado_desc|descuento_desc | limit=3
     */
    @Operation(summary = "Listar productos (filtros: categoria, q, oferta, excluir, ids, orden, limit)")
    @GetMapping
    public List<ProductoResponse> listar(
            @RequestParam(name = "categoria", required = false) String categoria,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "oferta", defaultValue = "false") boolean oferta,
            @RequestParam(name = "excluir", required = false) String excluir,
            @RequestParam(name = "ids", required = false) List<String> ids,
            @RequestParam(name = "orden", required = false) String orden,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return servicio.listar(categoria, q, oferta, excluir, ids, orden, limit)
            .stream().map(ProductoResponse::desde).toList();
    }

    @Operation(summary = "Obtener un producto por id")
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable("id") String id) {
        return ProductoResponse.desde(servicio.obtener(id));
    }

    @Operation(summary = "Crear un producto")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return ProductoResponse.desde(servicio.crear(request));
    }

    @Operation(summary = "Actualizar un producto")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable("id") String id, @Valid @RequestBody ProductoRequest request) {
        return ProductoResponse.desde(servicio.actualizar(id, request));
    }

    @Operation(summary = "Eliminar un producto")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") String id) {
        servicio.eliminar(id);
    }
}
