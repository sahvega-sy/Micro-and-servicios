package cl.clstore.resenas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResenaRequest(
    @JsonProperty("producto_id") @NotBlank(message = "es obligatorio") String productoId,
    @Min(value = 1, message = "debe ser entre 1 y 5") @Max(value = 5, message = "debe ser entre 1 y 5") int calificacion,
    @NotBlank(message = "es obligatorio") @Size(max = 1000, message = "máximo 1000 caracteres") String comentario
) {
}
