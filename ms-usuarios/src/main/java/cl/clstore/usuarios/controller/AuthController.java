package cl.clstore.usuarios.controller;

import cl.clstore.usuarios.dto.AuthResponse;
import cl.clstore.usuarios.dto.LoginRequest;
import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService servicio;

    public AuthController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return servicio.login(request);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registro(@Valid @RequestBody UsuarioRequest request) {
        return servicio.registrar(request);
    }
}
