package cl.clstore.carrito.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CantidadRequest(
    @NotNull(message = "es obligatoria") @Min(value = 1, message = "debe ser al menos 1") @Max(value = 99, message = "máximo 99") Integer cantidad
) {
}
