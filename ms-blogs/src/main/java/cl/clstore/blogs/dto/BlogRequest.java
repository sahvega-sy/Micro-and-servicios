package cl.clstore.blogs.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record BlogRequest(
    @Size(max = 100, message = "máximo 100 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "solo letras, números, guion y guion bajo") String id,
    @NotBlank(message = "es obligatorio") @Size(max = 255, message = "máximo 255 caracteres") String titulo,
    @NotBlank(message = "es obligatorio") @Size(max = 1000, message = "máximo 1000 caracteres") String resumen,
    @Size(max = 1000, message = "máximo 1000 caracteres") String imagen,
    @NotEmpty(message = "debe tener al menos un párrafo") List<String> contenido,
    @JsonProperty("fuente_nombre") @Size(max = 255, message = "máximo 255 caracteres") String fuenteNombre,
    @JsonProperty("fuente_url")
    @Size(max = 1000, message = "máximo 1000 caracteres")
    @Pattern(regexp = "^https?://.+", flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "debe comenzar con http:// o https://") String fuenteUrl
) {
}
