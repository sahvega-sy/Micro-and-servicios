package cl.clstore.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cuerpo para registrarse (POST /api/auth/registro), crear (POST /api/usuarios) y editar (PUT /api/usuarios/{id}).
 *  - "run" es obligatorio al crear/registrar y no se modifica al editar.
 *  - "password" es obligatoria al crear/registrar; al editar solo se cambia si viene informada.
 *  - "tipo" solo lo respeta la administración: en el registro público siempre se crea un Cliente.
 */
public record UsuarioRequest(
    @Size(max = 20, message = "máximo 20 caracteres") String run,
    @Pattern(regexp = "Administrador|Vendedor|Cliente", message = "debe ser Administrador, Vendedor o Cliente") String tipo,
    @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String nombre,
    @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String apellidos,
    @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") @Size(max = 150, message = "máximo 150 caracteres") String correo,
    @JsonProperty("fecha_nacimiento") LocalDate fechaNacimiento,
    @Size(min = 6, max = 72, message = "debe tener entre 6 y 72 caracteres") String password,
    @NotBlank(message = "es obligatoria") @Size(max = 100, message = "máximo 100 caracteres") String region,
    @NotBlank(message = "es obligatoria") @Size(max = 100, message = "máximo 100 caracteres") String comuna,
    @NotBlank(message = "es obligatoria") @Size(max = 255, message = "máximo 255 caracteres") String direccion
) {
}
