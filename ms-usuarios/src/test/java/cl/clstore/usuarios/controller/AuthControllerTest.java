package cl.clstore.usuarios.controller;

import static cl.clstore.usuarios.testsupport.DatosPrueba.RUN_VALIDO;
import static cl.clstore.usuarios.testsupport.DatosPrueba.usuarioResponse;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.usuarios.config.SecurityConfig;
import cl.clstore.usuarios.dto.AuthResponse;
import cl.clstore.usuarios.dto.LoginRequest;
import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.exception.ConflictoException;
import cl.clstore.usuarios.exception.NoAutorizadoException;
import cl.clstore.usuarios.exception.ReglaNegocioException;
import cl.clstore.usuarios.service.UsuarioService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba la capa HTTP de login y registro (rutas públicas): códigos de estado, validación del cuerpo y
 * formato de los errores. El servicio está simulado; la seguridad real (SecurityConfig) está cargada.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String REGISTRO_VALIDO = """
        {"run":"190110222","nombre":"Ana","apellidos":"Pérez","correo":"ana@gmail.com",
         "fecha_nacimiento":"1990-05-20","password":"clave123",
         "region":"Región Metropolitana de Santiago","comuna":"Santiago","direccion":"Calle Falsa 123"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService servicio;

    // -------------------------------------------------------------------- login

    @Test
    void login_credencialesValidas_devuelve200ConTokenYPerfilSinContrasena() throws Exception {
        when(servicio.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt-falso", usuarioResponse()));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"ana@gmail.com\",\"password\":\"clave123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-falso"))
            .andExpect(jsonPath("$.usuario.id").value("usuario-1"))
            .andExpect(jsonPath("$.usuario.run").value(RUN_VALIDO))
            .andExpect(jsonPath("$.usuario.correo").value("ana@gmail.com"))
            .andExpect(jsonPath("$.usuario.tipo").value("Cliente"))
            .andExpect(jsonPath("$.usuario.fecha_nacimiento").value("1990-05-20"))   // nombres JSON heredados del front
            .andExpect(jsonPath("$.usuario.created_at").value("2026-01-15T12:00:00Z"))
            .andExpect(jsonPath("$.usuario.password").doesNotExist())
            .andExpect(jsonPath("$.usuario.password_hash").doesNotExist())
            .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(servicio).login(captor.capture());
        assertEquals("ana@gmail.com", captor.getValue().correo());
        assertEquals("clave123", captor.getValue().password());
    }

    @Test
    void login_camposEnBlanco_devuelve400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.correo").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.password").value("es obligatoria"));

        verifyNoInteractions(servicio);
    }

    @Test
    void login_credencialesIncorrectas_devuelve401ConDetalle() throws Exception {
        when(servicio.login(any(LoginRequest.class))).thenThrow(new NoAutorizadoException("Correo o contraseña incorrectos."));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"ana@gmail.com\",\"password\":\"mala\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    void login_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es un JSON válido."));
    }

    // ----------------------------------------------------------------- registro

    @Test
    void registro_cuerpoValido_devuelve201YMapeaLosNombresJson() throws Exception {
        when(servicio.registrar(any(UsuarioRequest.class))).thenReturn(new AuthResponse("jwt-falso", usuarioResponse()));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("jwt-falso"))
            .andExpect(jsonPath("$.usuario.correo").value("ana@gmail.com"));

        ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);
        verify(servicio).registrar(captor.capture());
        assertEquals(RUN_VALIDO, captor.getValue().run());
        assertEquals("Pérez", captor.getValue().apellidos());
        assertEquals(LocalDate.of(1990, 5, 20), captor.getValue().fechaNacimiento());   // "fecha_nacimiento" -> fechaNacimiento
        assertEquals("clave123", captor.getValue().password());
    }

    @Test
    void registro_camposInvalidos_devuelve400ConErroresPorCampo() throws Exception {
        String cuerpo = "{\"nombre\":\"\",\"apellidos\":\"\",\"correo\":\"no-es-correo\",\"password\":\"123\","
            + "\"tipo\":\"Superadmin\",\"region\":\"\",\"comuna\":\"\",\"direccion\":\"\"}";

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.nombre").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.apellidos").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.correo").value("no es un correo válido"))
            .andExpect(jsonPath("$.errores.password").value("debe tener entre 6 y 72 caracteres"))
            .andExpect(jsonPath("$.errores.tipo").value("debe ser Administrador, Vendedor o Cliente"))
            .andExpect(jsonPath("$.errores.region").value("es obligatoria"))
            .andExpect(jsonPath("$.errores.comuna").value("es obligatoria"))
            .andExpect(jsonPath("$.errores.direccion").value("es obligatoria"));

        verifyNoInteractions(servicio);
    }

    @Test
    void registro_contrasenaDeMasDeSetentaYDosCaracteres_devuelve400() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(REGISTRO_VALIDO.replace("clave123", "x".repeat(73))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.password").value("debe tener entre 6 y 72 caracteres"));

        verifyNoInteractions(servicio);
    }

    @Test
    void registro_runDeMasDeVeinteCaracteres_devuelve400() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(REGISTRO_VALIDO.replace("190110222", "1".repeat(21))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.run").value("máximo 20 caracteres"));
    }

    @Test
    void registro_correoYaRegistrado_devuelve409() throws Exception {
        when(servicio.registrar(any(UsuarioRequest.class))).thenThrow(new ConflictoException("Ya existe un usuario con ese correo."));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe un usuario con ese correo."));
    }

    @Test
    void registro_reglaDeNegocio_devuelve400() throws Exception {
        when(servicio.registrar(any(UsuarioRequest.class)))
            .thenThrow(new ReglaNegocioException("El correo solo puede ser @duoc.cl, @profesor.duoc.cl o @gmail.com."));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("@gmail.com")));
    }
}
