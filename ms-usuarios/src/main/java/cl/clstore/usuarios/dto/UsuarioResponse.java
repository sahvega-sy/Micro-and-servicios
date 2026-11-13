package cl.clstore.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import cl.clstore.usuarios.model.Usuario;
import java.time.Instant;
import java.time.LocalDate;

/** Misma forma que la fila de "perfiles" en Supabase. Nunca incluye la contraseña. */
public record UsuarioResponse(
    String id,
    String run,
    String nombre,
    String apellidos,
    String correo,
    @JsonProperty("fecha_nacimiento") LocalDate fechaNacimiento,
    String tipo,
    String region,
    String comuna,
    String direccion,
    @JsonProperty("created_at") Instant createdAt
) {
    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getRun(), u.getNombre(), u.getApellidos(), u.getCorreo(),
            u.getFechaNacimiento(), u.getTipo(), u.getRegion(), u.getComuna(), u.getDireccion(), u.getCreatedAt());
    }
}
