package cl.clstore.usuarios.controller;

import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.dto.UsuarioResponse;
import cl.clstore.usuarios.service.UsuarioService;
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

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    /** Perfil del usuario del token (cualquier rol). Equivale a leer su fila de "perfiles". */
    @GetMapping("/me")
    public UsuarioResponse yo(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.desde(servicio.perfil(jwt.getSubject()));
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return servicio.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable("id") String id) {
        return UsuarioResponse.desde(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request, @AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.desde(servicio.crear(request, jwt.getClaimAsString("rol")));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable("id") String id, @Valid @RequestBody UsuarioRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.desde(servicio.actualizar(id, request, jwt.getClaimAsString("rol")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") String id, @AuthenticationPrincipal Jwt jwt) {
        servicio.eliminar(id, jwt.getSubject(), jwt.getClaimAsString("rol"));
    }
}
