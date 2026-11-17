package cl.clstore.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Para crear solo se envía la etiqueta (el "valor" se genera); el icono es opcional (clase Font Awesome). */
public record CategoriaRequest(
    @NotBlank(message = "es obligatoria") @Size(max = 100, message = "máximo 100 caracteres") String etiqueta,
    @Size(max = 50, message = "máximo 50 caracteres") String icono
) {
}
