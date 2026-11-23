package cl.clstore.resenas.controller;

import cl.clstore.resenas.dto.ResenaRequest;
import cl.clstore.resenas.dto.ResenaResponse;
import cl.clstore.resenas.service.ResenaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reseñas", description = "Reseñas de productos")
@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    private final ResenaService servicio;

    public ResenaController(ResenaService servicio) {
        this.servicio = servicio;
    }

    /** Público. Más recientes primero. Ej: GET /api/resenas?producto_id=rtx3080 */
    @Operation(summary = "Listar reseñas de un producto (público)")
    @GetMapping
    public List<ResenaResponse> listar(@RequestParam(name = "producto_id") String productoId) {
        return servicio.listarDeProducto(productoId).stream().map(ResenaResponse::desde).toList();
    }

    /** Requiere sesión (cualquier rol). El autor es el del token. */
    @Operation(summary = "Crear una reseña (el autor sale del token)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResenaResponse crear(@Valid @RequestBody ResenaRequest request, @AuthenticationPrincipal Jwt jwt) {
        String nombre = jwt.getClaimAsString("nombre");
        return ResenaResponse.desde(servicio.crear(request, nombre == null || nombre.isBlank() ? "Usuario" : nombre));
    }

    /** Moderación: Administrador o Vendedor. */
    @Operation(summary = "Eliminar una reseña (Administrador o Vendedor)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        servicio.eliminar(id);
    }
}
