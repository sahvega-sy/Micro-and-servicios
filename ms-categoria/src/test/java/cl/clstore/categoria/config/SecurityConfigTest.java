package cl.clstore.categoria.config;

import static cl.clstore.categoria.testsupport.DatosPrueba.categoria;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.categoria.controller.CategoriaController;
import cl.clstore.categoria.dto.CategoriaRequest;
import cl.clstore.categoria.service.CategoriaService;
import cl.clstore.categoria.testsupport.TokenDePrueba;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba las reglas de acceso con JWT HS256 reales (firmados con el mismo secreto que valida el servicio),
 * por lo que también verifica la conversión del claim "rol" a autoridad.
 */
@WebMvcTest(CategoriaController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String CATEGORIA_JSON = "{\"etiqueta\":\"Componentes\",\"icono\":\"fa-microchip\"}";

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private CategoriaService servicio;

    @BeforeEach
    void preparar() {
        when(servicio.obtener(anyString())).thenReturn(categoria());
        when(servicio.crear(any(CategoriaRequest.class))).thenReturn(categoria());
        when(servicio.actualizar(anyString(), any(CategoriaRequest.class))).thenReturn(categoria());
    }

    private String bearer(String rol) {
        return "Bearer " + TokenDePrueba.generar(secreto, rol, Duration.ofMinutes(5));
    }

    // ------------------------------------------------------------ rutas públicas

    @Test
    void getListado_esPublico() throws Exception {
        mvc.perform(get("/api/categorias")).andExpect(status().isOk());
    }

    @Test
    void getPorValor_esPublico() throws Exception {
        mvc.perform(get("/api/categorias/componentes")).andExpect(status().isOk());
    }

    // --------------------------------------------------------------- escritura

    @Test
    void crear_sinToken_devuelve401() throws Exception {
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_JSON))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,201", "Vendedor,201", "Cliente,403", "Servicio,403"})
    void crear_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(post("/api/categorias").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_JSON))
            .andExpect(status().is(esperado));
    }

    @Test
    void actualizar_sinToken_devuelve401() throws Exception {
        mvc.perform(put("/api/categorias/componentes").contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_JSON))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,200", "Vendedor,200", "Cliente,403"})
    void actualizar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(put("/api/categorias/componentes").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_JSON))
            .andExpect(status().is(esperado));
    }

    @Test
    void eliminar_sinToken_devuelve401() throws Exception {
        mvc.perform(delete("/api/categorias/redes")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,204", "Vendedor,204", "Cliente,403", "Servicio,403"})
    void eliminar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(delete("/api/categorias/redes").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    // ---------------------------------------------------------- tokens defectuosos

    @Test
    void token_firmadoConOtroSecreto_devuelve401() throws Exception {
        String falso = TokenDePrueba.generar("otro-secreto-distinto-de-mas-de-32-caracteres",
            "Administrador", Duration.ofMinutes(5));

        mvc.perform(delete("/api/categorias/redes").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_expirado_devuelve401() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Administrador", Duration.ofMinutes(-10));

        mvc.perform(delete("/api/categorias/redes").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_sinClaimRol_devuelve403() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(delete("/api/categorias/redes").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isForbidden());
    }

    @Test
    void token_basura_devuelve401() throws Exception {
        mvc.perform(delete("/api/categorias/redes").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------- CORS

    @Test
    void cors_origenPermitido_respondeElPreflight() throws Exception {
        mvc.perform(options("/api/categorias")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_origenNoPermitido_esRechazado() throws Exception {
        mvc.perform(options("/api/categorias")
                .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
