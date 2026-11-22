package cl.clstore.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/auth/google: el ID token (JWT) que entrega el botón "Iniciar sesión con Google". */
public record GoogleLoginRequest(
    @NotBlank(message = "es obligatorio") String credential
) {
}
