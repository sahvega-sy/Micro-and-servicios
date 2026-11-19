package cl.clstore.ordenes.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.ordenes.controller.OrdenController;
import cl.clstore.ordenes.dto.CrearOrdenResponse;
import cl.clstore.ordenes.dto.OrdenRequest;
import cl.clstore.ordenes.service.OrdenService;
import cl.clstore.ordenes.testsupport.DatosPrueba;
import cl.clstore.ordenes.testsupport.TokenDePrueba;
import java.math.BigDecimal;
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
 * comprar es público (invitado o con sesión), el detalle y "mis órdenes" requieren sesión y los listados
 * generales son solo del personal.
 */
@WebMvcTest(OrdenController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String ORDEN_JSON = """
        {"nombre":"Ana","apellidos":"Pérez","correo":"ana@gmail.com","region":"R","comuna":"C","direccion":"D",
         "items":[{"producto_id":"CPU-001","cantidad":1}]}""";

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private OrdenService servicio;

    @BeforeEach
    void preparar() {
        when(servicio.crear(any(OrdenRequest.class), any()))
            .thenReturn(new CrearOrdenResponse(1L, new BigDecimal("100"), "exitosa"));
        when(servicio.obtener(anyLong(), anyString(), anyBoolean())).thenReturn(DatosPrueba.ordenResponse());
    }

    private String bearer(String rol) {
        return "Bearer " + TokenDePrueba.generar(secreto, rol, Duration.ofMinutes(5));
    }

    // ------------------------------------------------------ compra (pública)

    @Test
    void crear_deInvitado_noRequiereTokenYNoAsociaUsuario() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(ORDEN_JSON))
            .andExpect(status().isCreated());

        verify(servicio).crear(any(OrdenRequest.class), isNull());
    }

    @ParameterizedTest
    @CsvSource({"Administrador", "Vendedor", "Cliente"})
    void crear_conSesion_asociaLaOrdenAlUsuarioDelToken(String rol) throws Exception {
        mvc.perform(post("/api/ordenes").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(ORDEN_JSON))
            .andExpect(status().isCreated());

        verify(servicio).crear(any(OrdenRequest.class), eq("usuario-prueba"));   // "sub" del token de TokenDePrueba
    }

    @Test
    void crear_conTokenExpirado_devuelve401_noSeTrataComoInvitado() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Cliente", Duration.ofMinutes(-10));

        mvc.perform(post("/api/ordenes").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado)
                .contentType(MediaType.APPLICATION_JSON).content(ORDEN_JSON))
            .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------- listados generales (personal)

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/ordenes")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,200", "Vendedor,200", "Cliente,403", "Servicio,403"})
    void listar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(get("/api/ordenes").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    @Test
    void items_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/ordenes/items")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,200", "Vendedor,200", "Cliente,403", "Servicio,403"})
    void items_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(get("/api/ordenes/items").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    // ------------------------------------------- mis órdenes y detalle (sesión)

    @Test
    void mias_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/ordenes/mias")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador", "Vendedor", "Cliente"})
    void mias_conCualquierRol_devuelve200(String rol) throws Exception {
        mvc.perform(get("/api/ordenes/mias").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().isOk());
    }

    @Test
    void detalle_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/ordenes/5")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador", "Vendedor", "Cliente"})
    void detalle_conCualquierRol_llegaAlControlador(String rol) throws Exception {
        mvc.perform(get("/api/ordenes/5").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().isOk());
    }

    // ---------------------------------------------------------- tokens defectuosos

    @Test
    void token_firmadoConOtroSecreto_devuelve401() throws Exception {
        String falso = TokenDePrueba.generar("otro-secreto-distinto-de-mas-de-32-caracteres",
            "Administrador", Duration.ofMinutes(5));

        mvc.perform(get("/api/ordenes").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_expirado_devuelve401() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Administrador", Duration.ofMinutes(-10));

        mvc.perform(get("/api/ordenes").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_sinClaimRol_devuelve403() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(get("/api/ordenes").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isForbidden());
    }

    @Test
    void token_basura_devuelve401() throws Exception {
        mvc.perform(get("/api/ordenes").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------- CORS

    @Test
    void cors_origenPermitido_respondeElPreflight() throws Exception {
        mvc.perform(options("/api/ordenes")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_origenNoPermitido_esRechazado() throws Exception {
        mvc.perform(options("/api/ordenes")
                .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
