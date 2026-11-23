package cl.clstore.usuarios.controller;

import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.dto.UsuarioResponse;
import cl.clstore.usuarios.service.UsuarioService;
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

@Tag(name = "Usuarios", description = "Gestión de usuarios. Requiere sesión; casi todo exige Administrador o Vendedor")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    /** Perfil del usuario del token (cualquier rol). Equivale a leer su fila de "perfiles". */
    @Operation(summary = "Datos del usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public UsuarioResponse yo(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.desde(servicio.perfil(jwt.getSubject()));
    }

    @Operation(summary = "Listar usuarios")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return servicio.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @Operation(summary = "Obtener un usuario por id")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable("id") String id) {
        return UsuarioResponse.desde(servicio.obtener(id));
    }

    @Operation(summary = "Crear un usuario")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request, @AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.desde(servicio.crear(request, jwt.getClaimAsString("rol")));
    }

    @Operation(summary = "Actualizar un usuario")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable("id") String id, @Valid @RequestBody UsuarioRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.desde(servicio.actualizar(id, request, jwt.getClaimAsString("rol")));
    }

    @Operation(summary = "Eliminar un usuario")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") String id, @AuthenticationPrincipal Jwt jwt) {
        servicio.eliminar(id, jwt.getSubject(), jwt.getClaimAsString("rol"));
    }
}
