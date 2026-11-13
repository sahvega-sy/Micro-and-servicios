package cl.clstore.ordenes.controller;

import cl.clstore.ordenes.dto.CrearOrdenResponse;
import cl.clstore.ordenes.dto.ItemOrdenResponse;
import cl.clstore.ordenes.dto.OrdenRequest;
import cl.clstore.ordenes.dto.OrdenResponse;
import cl.clstore.ordenes.service.OrdenService;
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

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService servicio;

    public OrdenController(OrdenService servicio) {
        this.servicio = servicio;
    }

    /** Público: acepta compras de invitado. Si llega un JWT válido, la orden queda asociada a ese usuario. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrearOrdenResponse crear(@Valid @RequestBody OrdenRequest request, @AuthenticationPrincipal Jwt jwt) {
        return servicio.crear(request, jwt == null ? null : jwt.getSubject());
    }

    /** Personal: todas las órdenes, opcionalmente filtradas con ?estado=exitosa|fallida. */
    @GetMapping
    public List<OrdenResponse> listar(@RequestParam(name = "estado", required = false) String estado) {
        return servicio.listar(estado);
    }

    /** Personal: todas las líneas de todas las órdenes (alimenta los reportes). */
    @GetMapping("/items")
    public List<ItemOrdenResponse> items() {
        return servicio.todosLosItems();
    }

    /** Las órdenes del usuario autenticado. */
    @GetMapping("/mias")
    public List<OrdenResponse> mias(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listarDeUsuario(jwt.getSubject());
    }

    /** Detalle (con "items") de la orden: su dueño o el personal. */
    @GetMapping("/{id}")
    public OrdenResponse obtener(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        String rol = jwt.getClaimAsString("rol");
        boolean esPersonal = "Administrador".equals(rol) || "Vendedor".equals(rol);
        return servicio.obtener(id, jwt.getSubject(), esPersonal);
    }
}
