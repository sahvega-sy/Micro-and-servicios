package cl.clstore.productos.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record ProductoRequest(
    @Size(min = 3, max = 100, message = "el código debe tener entre 3 y 100 caracteres") String id,
    @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String nombre,
    @Size(max = 500, message = "máximo 500 caracteres") String descripcion,
    @NotNull(message = "es obligatorio") @DecimalMin(value = "0.0", message = "no puede ser negativo") BigDecimal precio,
    @NotNull(message = "es obligatorio") @Min(value = 0, message = "no puede ser negativo") Integer stock,
    @JsonProperty("stock_critico") @Min(value = 0, message = "no puede ser negativo") Integer stockCritico,
    @NotBlank(message = "es obligatoria") String categoria,
    @Size(max = 1000, message = "máximo 1000 caracteres") String imagen,
    List<String> imagenes,
    Map<String, Object> destacado,
    Map<String, Object> especificaciones,
    @JsonProperty("descuento_porcentaje")
    @Min(value = 1, message = "debe ser entre 1 y 95")
    @Max(value = 95, message = "debe ser entre 1 y 95") Integer descuentoPorcentaje
) {
}
