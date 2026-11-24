package cl.clstore.blogs.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.blogs.controller.BlogController;
import cl.clstore.blogs.controller.ImagenController;
import cl.clstore.blogs.dto.BlogRequest;
import cl.clstore.blogs.service.AlmacenImagenes;
import cl.clstore.blogs.service.BlogService;
import cl.clstore.blogs.testsupport.DatosPrueba;
import cl.clstore.blogs.testsupport.TokenDePrueba;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

/**
 * Prueba las reglas de acceso con JWT HS256 reales (firmados con el mismo secreto que valida el servicio),
 * por lo que también verifica la conversión del claim "rol" a autoridad.
 */
@WebMvcTest(controllers = {BlogController.class, ImagenController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String BLOG_JSON =
        "{\"id\":\"mi-blog\",\"titulo\":\"Mi blog\",\"resumen\":\"Resumen\",\"contenido\":[\"Uno\"]}";

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private BlogService servicio;
    @MockitoBean
    private AlmacenImagenes almacen;

    @BeforeEach
    void preparar() {
        when(servicio.obtener(anyString())).thenReturn(DatosPrueba.blog());
        when(servicio.crear(any(BlogRequest.class))).thenReturn(DatosPrueba.blog());
        when(servicio.actualizar(anyString(), any(BlogRequest.class))).thenReturn(DatosPrueba.blog());
        when(almacen.guardar(any(MultipartFile.class))).thenReturn("http://localhost/uploads/blogs/x.png");
    }

    private String bearer(String rol) {
        return "Bearer " + TokenDePrueba.generar(secreto, rol, Duration.ofMinutes(5));
    }

    private static MockMultipartFile imagen() {
        return new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1});
    }

    // ------------------------------------------------------------ rutas públicas

    @Test
    void getListado_esPublico() throws Exception {
        mvc.perform(get("/api/blogs")).andExpect(status().isOk());
    }

    @Test
    void getPorId_esPublico() throws Exception {
        mvc.perform(get("/api/blogs/mi-blog")).andExpect(status().isOk());
    }

    // --------------------------------------------------------------- escritura

    @Test
    void crear_sinToken_devuelve401() throws Exception {
        mvc.perform(post("/api/blogs").contentType(MediaType.APPLICATION_JSON).content(BLOG_JSON))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,201", "Vendedor,201", "Cliente,403", "Servicio,403"})
    void crear_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(post("/api/blogs").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(BLOG_JSON))
            .andExpect(status().is(esperado));
    }

    @ParameterizedTest
    @CsvSource({"Administrador,200", "Vendedor,200", "Cliente,403"})
    void actualizar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(put("/api/blogs/mi-blog").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(BLOG_JSON))
            .andExpect(status().is(esperado));
    }

    @Test
    void eliminar_sinToken_devuelve401() throws Exception {
        mvc.perform(delete("/api/blogs/mi-blog")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,204", "Vendedor,204", "Cliente,403", "Servicio,403"})
    void eliminar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(delete("/api/blogs/mi-blog").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    // ------------------------------------------------------------------ imágenes

    @Test
    void subirImagen_sinToken_devuelve401() throws Exception {
        mvc.perform(multipart("/api/blogs/imagenes").file(imagen())).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,201", "Vendedor,201", "Cliente,403"})
    void subirImagen_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(multipart("/api/blogs/imagenes").file(imagen())
                .header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    // ---------------------------------------------------------- tokens defectuosos

    @Test
    void token_firmadoConOtroSecreto_devuelve401() throws Exception {
        String falso = TokenDePrueba.generar("otro-secreto-distinto-de-mas-de-32-caracteres",
            "Administrador", Duration.ofMinutes(5));

        mvc.perform(delete("/api/blogs/mi-blog").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_expirado_devuelve401() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Administrador", Duration.ofMinutes(-10));

        mvc.perform(delete("/api/blogs/mi-blog").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_sinClaimRol_devuelve403() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(delete("/api/blogs/mi-blog").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isForbidden());
    }

    @Test
    void token_basura_devuelve401() throws Exception {
        mvc.perform(delete("/api/blogs/mi-blog").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------- CORS

    @Test
    void cors_origenPermitido_respondeElPreflight() throws Exception {
        mvc.perform(options("/api/blogs")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_origenNoPermitido_esRechazado() throws Exception {
        mvc.perform(options("/api/blogs")
                .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
