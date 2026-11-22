package cl.clstore.blogs.testsupport;

import cl.clstore.blogs.dto.BlogRequest;
import cl.clstore.blogs.model.Blog;
import java.time.Instant;
import java.util.List;

public final class DatosPrueba {

    private DatosPrueba() {
    }

    public static Blog blog(String id, String titulo, Instant creado) {
        Blog b = new Blog();
        b.setId(id);
        b.setTitulo(titulo);
        b.setResumen("Resumen de " + titulo);
        b.setImagen("/img/Logo.png");
        b.setContenido(List.of("Primer párrafo.", "Segundo párrafo."));
        b.setFuenteNombre("Fuente");
        b.setFuenteUrl("https://ejemplo.cl/nota");
        b.setCreatedAt(creado);
        return b;
    }

    public static Blog blog() {
        return blog("mi-blog", "Mi blog", Instant.parse("2026-01-15T12:00:00Z"));
    }


    public static BlogRequest request(String id, String titulo, List<String> contenido) {
        return new BlogRequest(id, titulo, "Resumen", null, contenido, null, null);
    }
}
