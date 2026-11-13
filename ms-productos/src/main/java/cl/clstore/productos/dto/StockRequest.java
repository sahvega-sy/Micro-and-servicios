package cl.clstore.productos.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record StockRequest(@NotEmpty(message = "debe incluir al menos un producto") List<@Valid Item> items) {

    public record Item(
        @NotBlank(message = "es obligatorio") String id,
        @Min(value = 1, message = "debe ser al menos 1") int cantidad
    ) {
    }
}
