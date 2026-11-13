package cl.clstore.resenas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import cl.clstore.resenas.model.Resena;
import java.time.LocalDate;

/** Misma forma que las filas de "resenas" en Supabase. */
public record ResenaResponse(
    Long id,
    @JsonProperty("producto_id") String productoId,
    String usuario,
    int calificacion,
    String comentario,
    LocalDate fecha
) {
    public static ResenaResponse desde(Resena r) {
        return new ResenaResponse(r.getId(), r.getProductoId(), r.getUsuario(), r.getCalificacion(), r.getComentario(), r.getFecha());
    }
}
