package cl.clstore.blogs.service;

import static cl.clstore.blogs.testsupport.DatosPrueba.blog;
import static cl.clstore.blogs.testsupport.DatosPrueba.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.clstore.blogs.dto.BlogRequest;
import cl.clstore.blogs.exception.ConflictoException;
import cl.clstore.blogs.exception.RecursoNoEncontradoException;
import cl.clstore.blogs.exception.ReglaNegocioException;
import cl.clstore.blogs.model.Blog;
import cl.clstore.blogs.repository.BlogRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepository repositorio;

    @InjectMocks
    private BlogService servicio;

    // ----------------------------------------------------------------- listar

    @Test
    void listar_pideLosBlogsDelMasRecienteAlMasAntiguo() {
        List<Blog> esperados = List.of(blog());
        when(repositorio.findAll(any(Sort.class))).thenReturn(esperados);

        assertSame(esperados, servicio.listar());

        ArgumentCaptor<Sort> orden = ArgumentCaptor.forClass(Sort.class);
        verify(repositorio).findAll(orden.capture());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), orden.getValue());
    }

    // ---------------------------------------------------------------- obtener

    @Test
    void obtener_blogExistente_loDevuelve() {
        Blog esperado = blog();
        when(repositorio.findById("mi-blog")).thenReturn(Optional.of(esperado));

        assertSame(esperado, servicio.obtener("mi-blog"));
    }

    @Test
    void obtener_blogInexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.obtener("fantasma"));

        assertEquals("No existe el blog 'fantasma'.", ex.getMessage());
    }

    // ------------------------------------------------------------------ crear

    @Test
    void crear_datosValidos_guardaElBlogNormalizado() {
        when(repositorio.existsById("mi-blog")).thenReturn(false);
        when(repositorio.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));
        BlogRequest r = new BlogRequest(" mi-blog ", "  Mi título ", "  Resumen  ", "  http://x/img.png  ",
            List.of("  Uno  ", "Dos"), "  Diario  ", "  https://ejemplo.cl/nota  ");

        Blog resultado = servicio.crear(r);

        assertEquals("mi-blog", resultado.getId());                       // id sin espacios
        assertEquals("Mi título", resultado.getTitulo());
        assertEquals("Resumen", resultado.getResumen());
        assertEquals("http://x/img.png", resultado.getImagen());
        assertEquals(List.of("Uno", "Dos"), resultado.getContenido());
        assertEquals("Diario", resultado.getFuenteNombre());
        assertEquals("https://ejemplo.cl/nota", resultado.getFuenteUrl());
    }

    @Test
    void crear_camposOpcionalesVacios_seGuardanComoNull() {
        when(repositorio.existsById("mi-blog")).thenReturn(false);
        when(repositorio.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));
        BlogRequest r = new BlogRequest("mi-blog", "Título", "Resumen", "   ", List.of("Uno"), "", "  ");

        Blog resultado = servicio.crear(r);

        assertNull(resultado.getImagen());
        assertNull(resultado.getFuenteNombre());
        assertNull(resultado.getFuenteUrl());
    }

    @Test
    void crear_descartaLosParrafosVaciosONulos() {
        when(repositorio.existsById("mi-blog")).thenReturn(false);
        when(repositorio.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        Blog resultado = servicio.crear(request("mi-blog", "Título", Arrays.asList("Uno", "  ", null, "", " Dos ")));

        assertEquals(List.of("Uno", "Dos"), resultado.getContenido());
    }

    @Test
    void crear_idNull_lanzaReglaNegocio() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> servicio.crear(request(null, "Título", List.of("Uno"))));

        assertEquals("El identificador del blog es obligatorio.", ex.getMessage());
        verifyNoInteractions(repositorio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void crear_idEnBlanco_lanzaReglaNegocio(String id) {
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(request(id, "Título", List.of("Uno"))));

        verifyNoInteractions(repositorio);
    }

    @Test
    void crear_idYaExistente_lanzaConflictoYNoGuarda() {
        when(repositorio.existsById("mi-blog")).thenReturn(true);

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> servicio.crear(request("mi-blog", "Título", List.of("Uno"))));

        assertEquals("Ya existe un blog con ese identificador.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_contenidoSoloConParrafosVacios_lanzaReglaNegocioYNoGuarda() {
        when(repositorio.existsById("mi-blog")).thenReturn(false);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> servicio.crear(request("mi-blog", "Título", List.of("  ", ""))));

        assertEquals("El contenido del blog no puede estar vacío.", ex.getMessage());
        verify(repositorio, never()).save(any());
    }

    // -------------------------------------------------------------- actualizar

    @Test
    void actualizar_reemplazaLosDatosPeroNoElId() {
        Blog existente = blog("mi-blog", "Título viejo", java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(repositorio.findById("mi-blog")).thenReturn(Optional.of(existente));
        when(repositorio.save(any(Blog.class))).thenAnswer(inv -> inv.getArgument(0));

        Blog resultado = servicio.actualizar("mi-blog",
            new BlogRequest("otro-id-que-se-ignora", " Título nuevo ", "Resumen nuevo", null,
                List.of("Nuevo párrafo"), null, null));

        assertEquals("mi-blog", resultado.getId());
        assertEquals("Título nuevo", resultado.getTitulo());
        assertEquals("Resumen nuevo", resultado.getResumen());
        assertEquals(List.of("Nuevo párrafo"), resultado.getContenido());
        assertNull(resultado.getImagen());                                // un valor vacío borra la imagen anterior
        assertEquals(java.time.Instant.parse("2026-01-01T00:00:00Z"), resultado.getCreatedAt());
    }

    @Test
    void actualizar_blogInexistente_lanzaRecursoNoEncontradoYNoGuarda() {
        when(repositorio.findById("fantasma")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.actualizar("fantasma", request(null, "Título", List.of("Uno"))));

        verify(repositorio, never()).save(any());
    }

    @Test
    void actualizar_contenidoVacio_lanzaReglaNegocioYNoGuarda() {
        when(repositorio.findById("mi-blog")).thenReturn(Optional.of(blog()));

        assertThrows(ReglaNegocioException.class,
            () -> servicio.actualizar("mi-blog", request(null, "Título", List.of(" "))));

        verify(repositorio, never()).save(any());
    }

    // --------------------------------------------------------------- eliminar

    @Test
    void eliminar_blogExistente_loBorra() {
        when(repositorio.existsById("mi-blog")).thenReturn(true);

        servicio.eliminar("mi-blog");

        verify(repositorio).deleteById("mi-blog");
    }

    @Test
    void eliminar_blogInexistente_lanzaRecursoNoEncontradoYNoBorra() {
        when(repositorio.existsById("fantasma")).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> servicio.eliminar("fantasma"));

        assertEquals("No existe el blog 'fantasma'.", ex.getMessage());
        verify(repositorio, never()).deleteById(any());
    }
}
