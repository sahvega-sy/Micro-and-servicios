package cl.clstore.ordenes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import cl.clstore.ordenes.model.OrdenItem;
import java.math.BigDecimal;

public record ItemOrdenResponse(
    Long id,
    @JsonProperty("orden_id") Long ordenId,
    @JsonProperty("producto_id") String productoId,
    @JsonProperty("nombre_producto") String nombreProducto,
    int cantidad,
    @JsonProperty("precio_unitario") BigDecimal precioUnitario
) {
    public static ItemOrdenResponse desde(OrdenItem i) {
        return new ItemOrdenResponse(i.getId(), i.getOrdenId(), i.getProductoId(), i.getNombreProducto(),
            i.getCantidad(), i.getPrecioUnitario());
    }
}
