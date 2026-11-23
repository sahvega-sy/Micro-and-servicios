package cl.clstore.carrito.controller;

import cl.clstore.carrito.dto.AgregarItemRequest;
import cl.clstore.carrito.dto.CantidadRequest;
import cl.clstore.carrito.dto.ItemCarritoResponse;
import cl.clstore.carrito.service.CarritoService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** El carrito siempre es el del usuario del token (claim "sub"); no se puede consultar el de otro. */
@Tag(name = "Carrito", description = "Carrito de compras del usuario autenticado (cualquier rol)")
@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService servicio;

    public CarritoController(CarritoService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Ver el carrito")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public List<ItemCarritoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(jwt.getSubject());
    }

    /** Agrega (o suma) un producto. Devuelve el carrito actualizado. */
    @Operation(summary = "Agregar un producto al carrito")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/items")
    public List<ItemCarritoResponse> agregar(@Valid @RequestBody AgregarItemRequest request, @AuthenticationPrincipal Jwt jwt) {
        return servicio.agregar(jwt.getSubject(), request.productoId(), request.cantidad());
    }

    @Operation(summary = "Cambiar la cantidad de un producto del carrito")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/items/{productoId}")
    public List<ItemCarritoResponse> cambiarCantidad(@PathVariable("productoId") String productoId,
                                                     @Valid @RequestBody CantidadRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return servicio.cambiarCantidad(jwt.getSubject(), productoId, request.cantidad());
    }

    @Operation(summary = "Quitar un producto del carrito")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/items/{productoId}")
    public List<ItemCarritoResponse> eliminar(@PathVariable("productoId") String productoId, @AuthenticationPrincipal Jwt jwt) {
        return servicio.eliminar(jwt.getSubject(), productoId);
    }

    @Operation(summary = "Vaciar el carrito")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vaciar(@AuthenticationPrincipal Jwt jwt) {
        servicio.vaciar(jwt.getSubject());
    }
}
