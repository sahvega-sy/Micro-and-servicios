package cl.clstore.productos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Cuerpo de POST /api/productos/stock/descontar y /reponer (uso interno entre microservicios). */
public record StockRequest(@NotEmpty(message = "debe incluir al menos un producto") List<@Valid Item> items) {

    public record Item(
        @NotBlank(message = "es obligatorio") String id,
        @Min(value = 1, message = "debe ser al menos 1") int cantidad
    ) {
    }
}
