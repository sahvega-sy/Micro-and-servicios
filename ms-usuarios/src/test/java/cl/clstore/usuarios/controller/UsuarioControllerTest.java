package cl.clstore.usuarios.controller;

import static cl.clstore.usuarios.testsupport.DatosPrueba.USUARIO_ID;
import static cl.clstore.usuarios.testsupport.DatosPrueba.usuario;
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

import cl.clstore.usuarios.config.SecurityConfig;
import cl.clstore.usuarios.dto.UsuarioRequest;
import cl.clstore.usuarios.exception.ConflictoException;
import cl.clstore.usuarios.exception.NoAutorizadoException;
import cl.clstore.usuarios.exception.ProhibidoException;
import cl.clstore.usuarios.exception.RecursoNoEncontradoException;
import cl.clstore.usuarios.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Prueba la capa HTTP de la gestión de usuarios: rutas, códigos de estado, validación del cuerpo y errores.
 * El servicio está simulado; la seguridad real (SecurityConfig) está cargada y se autentica con jwt().
 */
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    private static final String CUERPO_VALIDO = """
        {"run":"190110222","tipo":"Vendedor","nombre":"Ana","apellidos":"Pérez","correo":"ana@gmail.com",
         "fecha_nacimiento":"1990-05-20","password":"clave123",
         "region":"Región Metropolitana de Santiago","comuna":"Santiago","direccion":"Calle Falsa 123"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService servicio;

    /** Sesión con id (claim "sub") USUARIO_ID y el rol indicado, que viaja como claim y como autoridad. */
    private static RequestPostProcessor sesion(String rol) {
        return jwt().jwt(j -> j.subject(USUARIO_ID).claim("rol", rol))
            .authorities(AuthorityUtils.createAuthorityList("ROLE_" + rol));
    }

    // ---------------------------------------------------------------- GET /me

    @Test
    void yo_devuelveElPerfilDelUsuarioDelToken() throws Exception {
        when(servicio.perfil(USUARIO_ID)).thenReturn(usuario());

        mvc.perform(get("/api/usuarios/me").with(sesion("Cliente")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USUARIO_ID))
            .andExpect(jsonPath("$.correo").value("ana@gmail.com"))
            .andExpect(jsonPath("$.tipo").value("Cliente"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(servicio).perfil(USUARIO_ID);
    }

    @Test
    void yo_usuarioEliminado_devuelve401() throws Exception {
        when(servicio.perfil(USUARIO_ID))
            .thenThrow(new NoAutorizadoException("La sesión ya no es válida. Vuelve a iniciar sesión."));

        mvc.perform(get("/api/usuarios/me").with(sesion("Cliente")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("La sesión ya no es válida. Vuelve a iniciar sesión."));
    }

    // ----------------------------------------------------------------- GET lista

    @Test
    void listar_devuelve200ConLosUsuariosSinContrasena() throws Exception {
        when(servicio.listar()).thenReturn(List.of(usuario(), usuario("id-2", "Vendedor", "luis@duoc.cl", "10000013K", "h")));

        mvc.perform(get("/api/usuarios").with(sesion("Administrador")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(USUARIO_ID))
            .andExpect(jsonPath("$[1].tipo").value("Vendedor"))
            .andExpect(jsonPath("$[0].password").doesNotExist())
            .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void obtener_usuarioExistente_devuelve200() throws Exception {
        when(servicio.obtener(USUARIO_ID)).thenReturn(usuario());

        mvc.perform(get("/api/usuarios/" + USUARIO_ID).with(sesion("Vendedor")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.run").value("190110222"));
    }

    @Test
    void obtener_usuarioInexistente_devuelve404() throws Exception {
        when(servicio.obtener("fantasma")).thenThrow(new RecursoNoEncontradoException("No existe el usuario 'fantasma'."));

        mvc.perform(get("/api/usuarios/fantasma").with(sesion("Administrador")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No existe el usuario 'fantasma'."));
    }

    // --------------------------------------------------------------------- POST

    @Test
    void crear_cuerpoValido_devuelve201YPasaElRolDelToken() throws Exception {
        when(servicio.crear(any(UsuarioRequest.class), eq("Vendedor"))).thenReturn(usuario());

        mvc.perform(post("/api/usuarios").with(sesion("Vendedor"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(USUARIO_ID));

        ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);
        verify(servicio).crear(captor.capture(), eq("Vendedor"));
        assertEquals("Vendedor", captor.getValue().tipo());
        assertEquals("190110222", captor.getValue().run());
    }

    @Test
    void crear_camposInvalidos_devuelve400ConErroresPorCampo() throws Exception {
        String cuerpo = "{\"nombre\":\"\",\"apellidos\":\"\",\"correo\":\"no-es-correo\",\"tipo\":\"Superadmin\","
            + "\"region\":\"\",\"comuna\":\"\",\"direccion\":\"\"}";

        mvc.perform(post("/api/usuarios").with(sesion("Administrador"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.nombre").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.correo").value("no es un correo válido"))
            .andExpect(jsonPath("$.errores.tipo").value("debe ser Administrador, Vendedor o Cliente"))
            .andExpect(jsonPath("$.errores.direccion").value("es obligatoria"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_vendedorIntentaCrearAdministrador_devuelve403() throws Exception {
        when(servicio.crear(any(UsuarioRequest.class), eq("Vendedor")))
            .thenThrow(new ProhibidoException("Solo un Administrador puede crear, modificar o eliminar cuentas de Administrador."));

        mvc.perform(post("/api/usuarios").with(sesion("Vendedor"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value(containsString("Solo un Administrador")));
    }

    @Test
    void crear_runRepetido_devuelve409() throws Exception {
        when(servicio.crear(any(UsuarioRequest.class), any())).thenThrow(new ConflictoException("Ya existe un usuario con ese RUN."));

        mvc.perform(post("/api/usuarios").with(sesion("Administrador"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe un usuario con ese RUN."));
    }

    // ---------------------------------------------------------------------- PUT

    @Test
    void actualizar_cuerpoValido_devuelve200YPasaElRolDelToken() throws Exception {
        when(servicio.actualizar(eq("id-2"), any(UsuarioRequest.class), eq("Administrador"))).thenReturn(usuario());

        mvc.perform(put("/api/usuarios/id-2").with(sesion("Administrador"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(USUARIO_ID));

        verify(servicio).actualizar(eq("id-2"), any(UsuarioRequest.class), eq("Administrador"));
    }

    @Test
    void actualizar_sinContrasena_esValido() throws Exception {
        when(servicio.actualizar(any(), any(UsuarioRequest.class), any())).thenReturn(usuario());

        mvc.perform(put("/api/usuarios/id-2").with(sesion("Administrador"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO.replace(",\"password\":\"clave123\"", "")))
            .andExpect(status().isOk());
    }

    @Test
    void actualizar_contrasenaDemasiadoCorta_devuelve400() throws Exception {
        mvc.perform(put("/api/usuarios/id-2").with(sesion("Administrador"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO.replace("clave123", "123")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.password").value("debe tener entre 6 y 72 caracteres"));

        verifyNoInteractions(servicio);
    }

    @Test
    void actualizar_usuarioInexistente_devuelve404() throws Exception {
        when(servicio.actualizar(eq("fantasma"), any(UsuarioRequest.class), any()))
            .thenThrow(new RecursoNoEncontradoException("No existe el usuario 'fantasma'."));

        mvc.perform(put("/api/usuarios/fantasma").with(sesion("Administrador"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_vendedorEditandoAdministrador_devuelve403() throws Exception {
        when(servicio.actualizar(eq("id-admin"), any(UsuarioRequest.class), eq("Vendedor")))
            .thenThrow(new ProhibidoException("Solo un Administrador puede crear, modificar o eliminar cuentas de Administrador."));

        mvc.perform(put("/api/usuarios/id-admin").with(sesion("Vendedor"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------- DELETE

    @Test
    void eliminar_devuelve204YPasaElIdDelActorYSuRol() throws Exception {
        mvc.perform(delete("/api/usuarios/id-2").with(sesion("Administrador")))
            .andExpect(status().isNoContent());

        verify(servicio).eliminar("id-2", USUARIO_ID, "Administrador");
    }

    @Test
    void eliminar_elPropioUsuario_devuelve409() throws Exception {
        doThrow(new ConflictoException("No puedes eliminar el usuario con el que iniciaste sesión."))
            .when(servicio).eliminar(USUARIO_ID, USUARIO_ID, "Administrador");

        mvc.perform(delete("/api/usuarios/" + USUARIO_ID).with(sesion("Administrador")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("No puedes eliminar el usuario con el que iniciaste sesión."));
    }

    @Test
    void eliminar_usuarioInexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe el usuario 'fantasma'."))
            .when(servicio).eliminar("fantasma", USUARIO_ID, "Administrador");

        mvc.perform(delete("/api/usuarios/fantasma").with(sesion("Administrador")))
            .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_vendedorIntentaBorrarAdministrador_devuelve403() throws Exception {
        doThrow(new ProhibidoException("Solo un Administrador puede crear, modificar o eliminar cuentas de Administrador."))
            .when(servicio).eliminar("id-admin", USUARIO_ID, "Vendedor");

        mvc.perform(delete("/api/usuarios/id-admin").with(sesion("Vendedor")))
            .andExpect(status().isForbidden());
    }
}
