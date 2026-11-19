package cl.clstore.carrito.config;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.carrito.controller.CarritoController;
import cl.clstore.carrito.service.CarritoService;
import cl.clstore.carrito.testsupport.TokenDePrueba;
import java.time.Duration;
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
 * El carrito es de cualquier usuario con sesión; todo lo demás exige personal (Administrador / Vendedor).
 */
@WebMvcTest(CarritoController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String ITEM_JSON = "{\"producto_id\":\"CPU-001\",\"cantidad\":1}";
    private static final String CANTIDAD_JSON = "{\"cantidad\":2}";

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private CarritoService servicio;

    private String bearer(String rol) {
        return "Bearer " + TokenDePrueba.generar(secreto, rol, Duration.ofMinutes(5));
    }

    // -------------------------------------------------------------- sin sesión

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/carrito")).andExpect(status().isUnauthorized());
    }

    @Test
    void agregar_sinToken_devuelve401() throws Exception {
        mvc.perform(post("/api/carrito/items").contentType(MediaType.APPLICATION_JSON).content(ITEM_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarCantidad_sinToken_devuelve401() throws Exception {
        mvc.perform(put("/api/carrito/items/CPU-001").contentType(MediaType.APPLICATION_JSON).content(CANTIDAD_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarProducto_sinToken_devuelve401() throws Exception {
        mvc.perform(delete("/api/carrito/items/CPU-001")).andExpect(status().isUnauthorized());
    }

    @Test
    void vaciar_sinToken_devuelve401() throws Exception {
        mvc.perform(delete("/api/carrito")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------- con sesión: cualquier rol sirve

    @ParameterizedTest
    @CsvSource({"Administrador", "Vendedor", "Cliente", "Servicio"})
    void listar_conCualquierRol_devuelve200(String rol) throws Exception {
        mvc.perform(get("/api/carrito").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().isOk());
    }

    @Test
    void agregar_conCliente_devuelve200() throws Exception {
        mvc.perform(post("/api/carrito/items").header(HttpHeaders.AUTHORIZATION, bearer("Cliente"))
                .contentType(MediaType.APPLICATION_JSON).content(ITEM_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void cambiarCantidad_conCliente_devuelve200() throws Exception {
        mvc.perform(put("/api/carrito/items/CPU-001").header(HttpHeaders.AUTHORIZATION, bearer("Cliente"))
                .contentType(MediaType.APPLICATION_JSON).content(CANTIDAD_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void vaciar_conCliente_devuelve204() throws Exception {
        mvc.perform(delete("/api/carrito").header(HttpHeaders.AUTHORIZATION, bearer("Cliente")))
            .andExpect(status().isNoContent());
    }

    @Test
    void token_sinClaimRol_igualPuedeUsarSuCarrito() throws Exception {
        // La regla del carrito es "authenticated()": basta una sesión válida, no se exige ningún rol.
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(get("/api/carrito").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isOk());
    }

    @Test
    void listar_usaElIdDelUsuarioDelToken() throws Exception {
        mvc.perform(get("/api/carrito").header(HttpHeaders.AUTHORIZATION, bearer("Cliente")))
            .andExpect(status().isOk());

        verify(servicio).listar("usuario-prueba");   // "sub" del token de TokenDePrueba
    }

    // ---------------------------------------------- cualquier otra ruta: personal

    @Test
    void rutaFueraDelCarrito_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/otra-cosa")).andExpect(status().isUnauthorized());
    }

    @Test
    void rutaFueraDelCarrito_conCliente_devuelve403() throws Exception {
        mvc.perform(get("/api/otra-cosa").header(HttpHeaders.AUTHORIZATION, bearer("Cliente")))
            .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------- tokens defectuosos

    @Test
    void token_firmadoConOtroSecreto_devuelve401() throws Exception {
        String falso = TokenDePrueba.generar("otro-secreto-distinto-de-mas-de-32-caracteres",
            "Cliente", Duration.ofMinutes(5));

        mvc.perform(get("/api/carrito").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_expirado_devuelve401() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Cliente", Duration.ofMinutes(-10));

        mvc.perform(get("/api/carrito").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_basura_devuelve401() throws Exception {
        mvc.perform(get("/api/carrito").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------- CORS

    @Test
    void cors_origenPermitido_respondeElPreflight() throws Exception {
        mvc.perform(options("/api/carrito/items")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_origenNoPermitido_esRechazado() throws Exception {
        mvc.perform(options("/api/carrito/items")
                .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
