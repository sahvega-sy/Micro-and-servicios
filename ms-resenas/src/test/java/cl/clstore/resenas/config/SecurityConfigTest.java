package cl.clstore.resenas.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.resenas.controller.ResenaController;
import cl.clstore.resenas.dto.ResenaRequest;
import cl.clstore.resenas.service.ResenaService;
import cl.clstore.resenas.testsupport.DatosPrueba;
import cl.clstore.resenas.testsupport.TokenDePrueba;
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
 * Prueba las reglas de acceso con JWT HS256 reales (firmados con el mismo secreto que valida el servicio):
 * leer es público, reseñar requiere sesión (cualquier rol) y eliminar es solo del personal.
 */
@WebMvcTest(ResenaController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String RESENA_JSON =
        "{\"producto_id\":\"CPU-001\",\"calificacion\":5,\"comentario\":\"Excelente\"}";

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private ResenaService servicio;

    @BeforeEach
    void preparar() {
        when(servicio.crear(any(ResenaRequest.class), anyString())).thenReturn(DatosPrueba.resena());
    }

    private String bearer(String rol) {
        return "Bearer " + TokenDePrueba.generar(secreto, rol, Duration.ofMinutes(5));
    }

    @Test
    void listar_esPublico() throws Exception {
        mvc.perform(get("/api/resenas").param("producto_id", "CPU-001")).andExpect(status().isOk());
    }

    @Test
    void crear_sinToken_devuelve401() throws Exception {
        mvc.perform(post("/api/resenas").contentType(MediaType.APPLICATION_JSON).content(RESENA_JSON))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,201", "Vendedor,201", "Cliente,201", "Servicio,201"})
    void crear_conCualquierRol_devuelve201(String rol, int esperado) throws Exception {
        mvc.perform(post("/api/resenas").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(RESENA_JSON))
            .andExpect(status().is(esperado));
    }

    @Test
    void crear_tokenSinClaimRol_igualFunciona() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(post("/api/resenas").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol)
                .contentType(MediaType.APPLICATION_JSON).content(RESENA_JSON))
            .andExpect(status().isCreated());
    }

    @Test
    void eliminar_sinToken_devuelve401() throws Exception {
        mvc.perform(delete("/api/resenas/1")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,204", "Vendedor,204", "Cliente,403", "Servicio,403"})
    void eliminar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(delete("/api/resenas/1").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    @Test
    void token_firmadoConOtroSecreto_devuelve401() throws Exception {
        String falso = TokenDePrueba.generar("otro-secreto-distinto-de-mas-de-32-caracteres",
            "Administrador", Duration.ofMinutes(5));

        mvc.perform(delete("/api/resenas/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_expirado_devuelve401() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Administrador", Duration.ofMinutes(-10));

        mvc.perform(delete("/api/resenas/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_sinClaimRol_noPuedeEliminar_devuelve403() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(delete("/api/resenas/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isForbidden());
    }

    @Test
    void token_basura_devuelve401() throws Exception {
        mvc.perform(delete("/api/resenas/1").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void cors_origenPermitido_respondeElPreflight() throws Exception {
        mvc.perform(options("/api/resenas")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_origenNoPermitido_esRechazado() throws Exception {
        mvc.perform(options("/api/resenas")
                .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
