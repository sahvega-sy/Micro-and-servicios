package cl.clstore.carrito.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** Datos del producto que necesita el carrito. Se lee de ms-productos y se devuelve tal cual al front. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoResumen(
    String id,
    String nombre,
    BigDecimal precio,
    String imagen,
    @JsonProperty("descuento_porcentaje") Integer descuentoPorcentaje,
    Integer stock
) {
}
