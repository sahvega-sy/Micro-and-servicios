package cl.clstore.contacto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactoRequest(
    @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String nombre,
    @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") @Size(max = 150, message = "máximo 150 caracteres") String email,
    @NotBlank(message = "es obligatorio") @Size(max = 2000, message = "máximo 2000 caracteres") String mensaje
) {
}
