package cl.clstore.blogs.repository;

import static cl.clstore.blogs.testsupport.DatosPrueba.blog;
import static org.assertj.core.api.Assertions.assertThat;

import cl.clstore.blogs.model.Blog;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Sort;

/**
 * OPCIONAL. Usa una base H2 en memoria (reemplaza a MySQL) para probar el mapeo de la columna JSON de
 * párrafos y el orden que usa el servicio. Cada test se revierte solo al terminar.
 */
@DataJpaTest
class BlogRepositoryTest {

    @Autowired
    private BlogRepository repositorio;

    @Autowired
    private TestEntityManager em;

    @Test
    void guardar_yLeerDeNuevo_conservaTodosLosCamposIncluidaLaListaDeParrafos() {
        Blog original = blog("mi-blog", "Título con ñ", Instant.parse("2026-01-15T12:00:00Z"));
        original.setContenido(List.of("Párrafo uno", "Párrafo \"dos\" con tildes: áéíóú", "Tres"));
        repositorio.saveAndFlush(original);
        em.clear();   // fuerza a leer de la base y no de la caché de la sesión

        Blog leido = repositorio.findById("mi-blog").orElseThrow();

        assertThat(leido.getTitulo()).isEqualTo("Título con ñ");
        assertThat(leido.getResumen()).isEqualTo("Resumen de Título con ñ");
        assertThat(leido.getContenido())
            .containsExactly("Párrafo uno", "Párrafo \"dos\" con tildes: áéíóú", "Tres");
        assertThat(leido.getFuenteNombre()).isEqualTo("Fuente");
        assertThat(leido.getFuenteUrl()).isEqualTo("https://ejemplo.cl/nota");
        assertThat(leido.getCreatedAt()).isEqualTo(Instant.parse("2026-01-15T12:00:00Z"));
    }

    @Test
    void guardar_sinFecha_asignaCreatedAtAutomaticamente() {
        Blog sinFecha = blog("sin-fecha", "Sin fecha", null);

        Blog guardado = repositorio.saveAndFlush(sinFecha);

        assertThat(guardado.getCreatedAt()).isNotNull();
    }

    @Test
    void guardar_camposOpcionalesNulos_sePersistenComoNulos() {
        Blog minimo = blog("minimo", "Mínimo", Instant.parse("2026-01-01T00:00:00Z"));
        minimo.setImagen(null);
        minimo.setFuenteNombre(null);
        minimo.setFuenteUrl(null);
        repositorio.saveAndFlush(minimo);
        em.clear();

        Blog leido = repositorio.findById("minimo").orElseThrow();

        assertThat(leido.getImagen()).isNull();
        assertThat(leido.getFuenteNombre()).isNull();
        assertThat(leido.getFuenteUrl()).isNull();
    }

    @Test
    void findAll_ordenadoPorCreatedAtDescendente_comoLoPideElServicio() {
        repositorio.saveAllAndFlush(List.of(
            blog("viejo", "Viejo", Instant.parse("2026-01-01T00:00:00Z")),
            blog("nuevo", "Nuevo", Instant.parse("2026-03-01T00:00:00Z")),
            blog("medio", "Medio", Instant.parse("2026-02-01T00:00:00Z"))));

        List<String> ids = repositorio.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream().map(Blog::getId).toList();

        assertThat(ids).containsExactly("nuevo", "medio", "viejo");
    }

    @Test
    void actualizarContenido_sePersiste() {
        repositorio.saveAndFlush(blog("mi-blog", "Título", Instant.parse("2026-01-15T12:00:00Z")));
        Blog b = repositorio.findById("mi-blog").orElseThrow();

        b.setContenido(List.of("Solo un párrafo nuevo"));
        repositorio.saveAndFlush(b);
        em.clear();

        assertThat(repositorio.findById("mi-blog").orElseThrow().getContenido()).containsExactly("Solo un párrafo nuevo");
    }

    @Test
    void existsByIdYEliminar_funcionanPorIdTexto() {
        repositorio.saveAndFlush(blog("mi-blog", "Título", Instant.parse("2026-01-15T12:00:00Z")));

        assertThat(repositorio.existsById("mi-blog")).isTrue();
        repositorio.deleteById("mi-blog");
        repositorio.flush();
        assertThat(repositorio.existsById("mi-blog")).isFalse();
    }
}
