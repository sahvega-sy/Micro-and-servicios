package cl.clstore.carrito.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** "cantidad" es opcional: si no viene se agrega 1 unidad. */
public record AgregarItemRequest(
    @JsonProperty("producto_id") @NotBlank(message = "es obligatorio") String productoId,
    @Min(value = 1, message = "debe ser al menos 1") @Max(value = 99, message = "máximo 99") Integer cantidad
) {
}
