package cl.clstore.carrito.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Misma forma que la consulta que hacía el front a Supabase: { id, producto_id, cantidad, productos: {...} }. */
public record ItemCarritoResponse(
    Long id,
    @JsonProperty("producto_id") String productoId,
    int cantidad,
    ProductoResumen productos
) {
}
