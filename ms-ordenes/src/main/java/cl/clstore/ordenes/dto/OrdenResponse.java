package cl.clstore.ordenes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import cl.clstore.ordenes.model.Orden;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Mismos nombres de columna que la tabla "ordenes" de Supabase. "items" solo viaja en el detalle. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrdenResponse(
    Long id,
    @JsonProperty("usuario_id") String usuarioId,
    @JsonProperty("nombre_comprador") String nombreComprador,
    @JsonProperty("apellidos_comprador") String apellidosComprador,
    @JsonProperty("correo_comprador") String correoComprador,
    String telefono,
    String region,
    String comuna,
    String direccion,
    BigDecimal total,
    String estado,
    @JsonProperty("creado_en") Instant creadoEn,
    List<ItemOrdenResponse> items
) {
    public static OrdenResponse desde(Orden o, boolean conItems) {
        List<ItemOrdenResponse> items = conItems ? o.getItems().stream().map(ItemOrdenResponse::desde).toList() : null;
        return new OrdenResponse(o.getId(), o.getUsuarioId(), o.getNombreComprador(), o.getApellidosComprador(),
            o.getCorreoComprador(), o.getTelefono(), o.getRegion(), o.getComuna(), o.getDireccion(),
            o.getTotal(), o.getEstado(), o.getCreadoEn(), items);
    }
}
