package cl.clstore.usuarios.config;

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

import cl.clstore.usuarios.controller.AuthController;
import cl.clstore.usuarios.controller.UsuarioController;
import cl.clstore.usuarios.dto.AuthResponse;
import cl.clstore.usuarios.dto.LoginRequest;
import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.service.UsuarioService;
import cl.clstore.usuarios.testsupport.DatosPrueba;
import cl.clstore.usuarios.testsupport.TokenDePrueba;
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
 * login y registro son públicos, /me requiere sesión y la gestión de usuarios es solo del personal.
 */
@WebMvcTest(controllers = {AuthController.class, UsuarioController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String LOGIN_JSON = "{\"correo\":\"ana@gmail.com\",\"password\":\"clave123\"}";
    private static final String USUARIO_JSON = """
        {"run":"190110222","nombre":"Ana","apellidos":"Pérez","correo":"ana@gmail.com","password":"clave123",
         "region":"Región Metropolitana de Santiago","comuna":"Santiago","direccion":"Calle Falsa 123"}""";

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        AuthResponse auth = new AuthResponse("jwt-falso", DatosPrueba.usuarioResponse());
        when(servicio.login(any(LoginRequest.class))).thenReturn(auth);
        when(servicio.registrar(any(UsuarioRequest.class))).thenReturn(auth);
        when(servicio.perfil(anyString())).thenReturn(DatosPrueba.usuario());
        when(servicio.obtener(anyString())).thenReturn(DatosPrueba.usuario());
        when(servicio.crear(any(UsuarioRequest.class), any())).thenReturn(DatosPrueba.usuario());
        when(servicio.actualizar(anyString(), any(UsuarioRequest.class), any())).thenReturn(DatosPrueba.usuario());
    }

    private String bearer(String rol) {
        return "Bearer " + TokenDePrueba.generar(secreto, rol, Duration.ofMinutes(5));
    }

    // ------------------------------------------------------------ rutas públicas

    @Test
    void login_esPublico() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void registro_esPublico() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(USUARIO_JSON))
            .andExpect(status().isCreated());
    }

    // ----------------------------------------------------------------------- /me

    @Test
    void me_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador", "Vendedor", "Cliente", "Servicio"})
    void me_conCualquierRol_devuelve200(String rol) throws Exception {
        mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().isOk());
    }

    @Test
    void me_tokenSinClaimRol_igualPuedeVerSuPerfil() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isOk());
    }

    // ------------------------------------------------- gestión de usuarios (personal)

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,200", "Vendedor,200", "Cliente,403", "Servicio,403"})
    void listar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    @ParameterizedTest
    @CsvSource({"Administrador,200", "Vendedor,200", "Cliente,403"})
    void obtenerPorId_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(get("/api/usuarios/usuario-1").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    @Test
    void crear_sinToken_devuelve401() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(USUARIO_JSON))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,201", "Vendedor,201", "Cliente,403", "Servicio,403"})
    void crear_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(post("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(USUARIO_JSON))
            .andExpect(status().is(esperado));
    }

    @ParameterizedTest
    @CsvSource({"Administrador,200", "Vendedor,200", "Cliente,403"})
    void actualizar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(put("/api/usuarios/usuario-1").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(USUARIO_JSON))
            .andExpect(status().is(esperado));
    }

    @Test
    void eliminar_sinToken_devuelve401() throws Exception {
        mvc.perform(delete("/api/usuarios/usuario-1")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,204", "Vendedor,204", "Cliente,403", "Servicio,403"})
    void eliminar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(delete("/api/usuarios/usuario-1").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    // ---------------------------------------------------------- tokens defectuosos

    @Test
    void token_firmadoConOtroSecreto_devuelve401() throws Exception {
        String falso = TokenDePrueba.generar("otro-secreto-distinto-de-mas-de-32-caracteres",
            "Administrador", Duration.ofMinutes(5));

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_expirado_devuelve401() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Administrador", Duration.ofMinutes(-10));

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_sinClaimRol_noPuedeGestionarUsuarios_devuelve403() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isForbidden());
    }

    @Test
    void token_basura_devuelve401() throws Exception {
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------- CORS

    @Test
    void cors_origenPermitido_respondeElPreflight() throws Exception {
        mvc.perform(options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_origenNoPermitido_esRechazado() throws Exception {
        mvc.perform(options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
