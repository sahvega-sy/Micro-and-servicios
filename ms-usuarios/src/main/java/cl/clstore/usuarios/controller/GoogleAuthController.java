package cl.clstore.usuarios.controller;

import cl.clstore.usuarios.dto.AuthResponse;
import cl.clstore.usuarios.dto.GoogleLoginRequest;
import cl.clstore.usuarios.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Registro y login. Devuelven el JWT que usan todos los servicios")
@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private final GoogleAuthService servicio;

    public GoogleAuthController(GoogleAuthService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Iniciar sesión con Google")
    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return servicio.login(request.credential());
    }
}
