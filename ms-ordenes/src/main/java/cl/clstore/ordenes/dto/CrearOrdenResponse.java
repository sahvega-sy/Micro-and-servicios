package cl.clstore.ordenes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** Equivale a lo que devolvía la función crear_orden: { orden_id, total }. */
public record CrearOrdenResponse(@JsonProperty("orden_id") Long ordenId, BigDecimal total, String estado) {
}
