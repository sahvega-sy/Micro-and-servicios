package cl.clstore.ordenes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** Datos del producto que necesita una orden, leídos de ms-productos. */
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
