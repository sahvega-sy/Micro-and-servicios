package cl.clstore.blogs.controller;

import static cl.clstore.blogs.testsupport.DatosPrueba.blog;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.blogs.config.SecurityConfig;
import cl.clstore.blogs.dto.BlogRequest;
import cl.clstore.blogs.exception.ConflictoException;
import cl.clstore.blogs.exception.RecursoNoEncontradoException;
import cl.clstore.blogs.exception.ReglaNegocioException;
import cl.clstore.blogs.service.BlogService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba la capa HTTP: rutas, códigos de estado, validación del cuerpo y formato de los errores.
 * El servicio está simulado; la seguridad real (SecurityConfig) está cargada y se autentica con jwt().
 */
@WebMvcTest(BlogController.class)
@Import(SecurityConfig.class)
class BlogControllerTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Administrador");

    private static final String CUERPO_VALIDO = """
        {"id":"mi-blog","titulo":"Mi blog","resumen":"Resumen","imagen":"/img/Logo.png",
         "contenido":["Primer párrafo.","Segundo párrafo."],
         "fuente_nombre":"Fuente","fuente_url":"https://ejemplo.cl/nota"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BlogService servicio;

    // ---------------------------------------------------------------- GET lista

    @Test
    void listar_devuelve200ConLosBlogs() throws Exception {
        when(servicio.listar()).thenReturn(List.of(blog(), blog("otro", "Otro", Instant.parse("2026-02-01T00:00:00Z"))));

        mvc.perform(get("/api/blogs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value("mi-blog"))
            .andExpect(jsonPath("$[0].titulo").value("Mi blog"))
            .andExpect(jsonPath("$[0].contenido", hasSize(2)))
            .andExpect(jsonPath("$[0].fuente_nombre").value("Fuente"))          // nombres JSON heredados del front
            .andExpect(jsonPath("$[0].fuente_url").value("https://ejemplo.cl/nota"))
            .andExpect(jsonPath("$[0].created_at").value("2026-01-15T12:00:00Z"));
    }

    // ----------------------------------------------------------------- GET {id}

    @Test
    void obtener_blogExistente_devuelve200() throws Exception {
        when(servicio.obtener("mi-blog")).thenReturn(blog());

        mvc.perform(get("/api/blogs/mi-blog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("mi-blog"))
            .andExpect(jsonPath("$.contenido[0]").value("Primer párrafo."));
    }

    @Test
    void obtener_blogInexistente_devuelve404ConDetalle() throws Exception {
        when(servicio.obtener("fantasma")).thenThrow(new RecursoNoEncontradoException("No existe el blog 'fantasma'."));

        mvc.perform(get("/api/blogs/fantasma"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No existe el blog 'fantasma'."));
    }

    // --------------------------------------------------------------------- POST

    @Test
    void crear_cuerpoValido_devuelve201YMapeaLosNombresJson() throws Exception {
        when(servicio.crear(any(BlogRequest.class))).thenReturn(blog());

        mvc.perform(post("/api/blogs").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("mi-blog"));

        ArgumentCaptor<BlogRequest> captor = ArgumentCaptor.forClass(BlogRequest.class);
        verify(servicio).crear(captor.capture());
        assertEquals("Mi blog", captor.getValue().titulo());
        assertEquals(List.of("Primer párrafo.", "Segundo párrafo."), captor.getValue().contenido());
        assertEquals("Fuente", captor.getValue().fuenteNombre());                // "fuente_nombre" -> fuenteNombre
        assertEquals("https://ejemplo.cl/nota", captor.getValue().fuenteUrl());
    }

    @Test
    void crear_camposInvalidos_devuelve400ConErroresPorCampo() throws Exception {
        String cuerpo = "{\"id\":\"con espacios\",\"titulo\":\"\",\"resumen\":\"\",\"contenido\":[],"
            + "\"fuente_url\":\"ftp://x\"}";

        mvc.perform(post("/api/blogs").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.id").value("solo letras, números, guion y guion bajo"))
            .andExpect(jsonPath("$.errores.titulo").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.resumen").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.contenido").value("debe tener al menos un párrafo"))
            .andExpect(jsonPath("$.errores.fuenteUrl").value("debe comenzar con http:// o https://"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_sinContenido_devuelve400() throws Exception {
        mvc.perform(post("/api/blogs").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"a\",\"titulo\":\"T\",\"resumen\":\"R\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.contenido").value("debe tener al menos un párrafo"));
    }

    @Test
    void crear_textosDemasiadoLargos_devuelve400() throws Exception {
        String cuerpo = "{\"id\":\"a\",\"titulo\":\"" + "t".repeat(256) + "\",\"resumen\":\"" + "r".repeat(1001)
            + "\",\"contenido\":[\"x\"]}";

        mvc.perform(post("/api/blogs").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.titulo").value("máximo 255 caracteres"))
            .andExpect(jsonPath("$.errores.resumen").value("máximo 1000 caracteres"));
    }

    @Test
    void crear_fuenteUrlConHttpEnMayusculas_esValida() throws Exception {
        when(servicio.crear(any(BlogRequest.class))).thenReturn(blog());
        String cuerpo = "{\"id\":\"a\",\"titulo\":\"T\",\"resumen\":\"R\",\"contenido\":[\"x\"],"
            + "\"fuente_url\":\"HTTPS://EJEMPLO.CL\"}";

        mvc.perform(post("/api/blogs").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isCreated());
    }

    @Test
    void crear_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/blogs").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es un JSON válido."));
    }

    @Test
    void crear_idRepetido_devuelve409() throws Exception {
        when(servicio.crear(any(BlogRequest.class)))
            .thenThrow(new ConflictoException("Ya existe un blog con ese identificador."));

        mvc.perform(post("/api/blogs").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe un blog con ese identificador."));
    }

    @Test
    void crear_reglaDeNegocio_devuelve400() throws Exception {
        when(servicio.crear(any(BlogRequest.class)))
            .thenThrow(new ReglaNegocioException("El contenido del blog no puede estar vacío."));

        mvc.perform(post("/api/blogs").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El contenido del blog no puede estar vacío."));
    }

    // ---------------------------------------------------------------------- PUT

    @Test
    void actualizar_cuerpoValido_devuelve200() throws Exception {
        when(servicio.actualizar(eq("mi-blog"), any(BlogRequest.class))).thenReturn(blog());

        mvc.perform(put("/api/blogs/mi-blog").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("mi-blog"));
    }

    @Test
    void actualizar_blogInexistente_devuelve404() throws Exception {
        when(servicio.actualizar(eq("fantasma"), any(BlogRequest.class)))
            .thenThrow(new RecursoNoEncontradoException("No existe el blog 'fantasma'."));

        mvc.perform(put("/api/blogs/fantasma").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_cuerpoInvalido_devuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(put("/api/blogs/mi-blog").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    // ------------------------------------------------------------------- DELETE

    @Test
    void eliminar_blogExistente_devuelve204() throws Exception {
        mvc.perform(delete("/api/blogs/mi-blog").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNoContent());

        verify(servicio).eliminar("mi-blog");
    }

    @Test
    void eliminar_blogInexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe el blog 'fantasma'."))
            .when(servicio).eliminar("fantasma");

        mvc.perform(delete("/api/blogs/fantasma").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNotFound());
    }
}
