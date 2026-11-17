package cl.clstore.blogs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import cl.clstore.blogs.model.Blog;
import java.time.Instant;
import java.util.List;

/** Misma forma que las filas que devolvía Supabase (fuente_nombre, fuente_url, created_at). */
public record BlogResponse(
    String id,
    String titulo,
    String resumen,
    String imagen,
    List<String> contenido,
    @JsonProperty("fuente_nombre") String fuenteNombre,
    @JsonProperty("fuente_url") String fuenteUrl,
    @JsonProperty("created_at") Instant createdAt
) {
    public static BlogResponse desde(Blog b) {
        return new BlogResponse(b.getId(), b.getTitulo(), b.getResumen(), b.getImagen(),
            b.getContenido(), b.getFuenteNombre(), b.getFuenteUrl(), b.getCreatedAt());
    }
}
