package cl.clstore.ordenes.controller;

import cl.clstore.ordenes.dto.CrearOrdenResponse;
import cl.clstore.ordenes.dto.ItemOrdenResponse;
import cl.clstore.ordenes.dto.OrdenRequest;
import cl.clstore.ordenes.dto.OrdenResponse;
import cl.clstore.ordenes.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Órdenes", description = "Compra y consulta de órdenes")
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService servicio;

    public OrdenController(OrdenService servicio) {
        this.servicio = servicio;
    }

    /** Público: acepta compras de invitado. Si llega un JWT válido, la orden queda asociada a ese usuario. */
    @Operation(summary = "Crear una orden (público; con token queda a nombre del usuario). Valida precios y descuenta stock")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrearOrdenResponse crear(@Valid @RequestBody OrdenRequest request, @AuthenticationPrincipal Jwt jwt) {
        return servicio.crear(request, jwt == null ? null : jwt.getSubject());
    }

    /** Personal: todas las órdenes, opcionalmente filtradas con ?estado=exitosa|fallida. */
    @Operation(summary = "Listar órdenes, filtrable por estado (Administrador o Vendedor)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public List<OrdenResponse> listar(@RequestParam(name = "estado", required = false) String estado) {
        return servicio.listar(estado);
    }

    /** Personal: todas las líneas de todas las órdenes (alimenta los reportes). */
    @Operation(summary = "Listar todas las líneas de órdenes (Administrador o Vendedor)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/items")
    public List<ItemOrdenResponse> items() {
        return servicio.todosLosItems();
    }

    /** Las órdenes del usuario autenticado. */
    @Operation(summary = "Listar las órdenes del usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mias")
    public List<OrdenResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listarDeUsuario(jwt.getSubject());
    }

    /** Detalle (con "items") de la orden: su dueño o el personal. */
    @Operation(summary = "Obtener una orden con sus items (su dueño o el personal)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public OrdenResponse obtener(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        String rol = jwt.getClaimAsString("rol");
        boolean esPersonal = "Administrador".equals(rol) || "Vendedor".equals(rol);
        return servicio.obtener(id, jwt.getSubject(), esPersonal);
    }
}
