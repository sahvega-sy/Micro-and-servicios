package cl.clstore.contacto.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.contacto.controller.ContactoController;
import cl.clstore.contacto.dto.ContactoRequest;
import cl.clstore.contacto.service.ContactoService;
import cl.clstore.contacto.testsupport.DatosPrueba;
import cl.clstore.contacto.testsupport.TokenDePrueba;
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
@WebMvcTest(ContactoController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String CONTACTO_JSON =
        "{\"nombre\":\"Ana\",\"email\":\"ana@gmail.com\",\"mensaje\":\"Hola\"}";

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private ContactoService servicio;

    @BeforeEach
    void preparar() {
        when(servicio.crear(any(ContactoRequest.class))).thenReturn(DatosPrueba.contacto());
    }

    private String bearer(String rol) {
        return "Bearer " + TokenDePrueba.generar(secreto, rol, Duration.ofMinutes(5));
    }

    // ------------------------------------------------- formulario público (POST)

    @Test
    void crear_esPublico_noRequiereToken() throws Exception {
        mvc.perform(post("/api/contactos").contentType(MediaType.APPLICATION_JSON).content(CONTACTO_JSON))
            .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @CsvSource({"Administrador", "Vendedor", "Cliente"})
    void crear_tambienFuncionaConSesionIniciada(String rol) throws Exception {
        mvc.perform(post("/api/contactos").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(CONTACTO_JSON))
            .andExpect(status().isCreated());
    }

    // ------------------------------------------------------ bandeja (solo personal)

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/contactos")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,200", "Vendedor,200", "Cliente,403", "Servicio,403"})
    void listar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(get("/api/contactos").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    @Test
    void eliminar_sinToken_devuelve401() throws Exception {
        mvc.perform(delete("/api/contactos/1")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,204", "Vendedor,204", "Cliente,403", "Servicio,403"})
    void eliminar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(delete("/api/contactos/1").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    // ---------------------------------------------------------- tokens defectuosos

    @Test
    void token_firmadoConOtroSecreto_devuelve401() throws Exception {
        String falso = TokenDePrueba.generar("otro-secreto-distinto-de-mas-de-32-caracteres",
            "Administrador", Duration.ofMinutes(5));

        mvc.perform(get("/api/contactos").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_expirado_devuelve401() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Administrador", Duration.ofMinutes(-10));

        mvc.perform(get("/api/contactos").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_sinClaimRol_devuelve403() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(get("/api/contactos").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isForbidden());
    }

    @Test
    void token_basura_devuelve401() throws Exception {
        mvc.perform(get("/api/contactos").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------- CORS

    @Test
    void cors_origenPermitido_respondeElPreflight() throws Exception {
        mvc.perform(options("/api/contactos")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_origenNoPermitido_esRechazado() throws Exception {
        mvc.perform(options("/api/contactos")
                .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
