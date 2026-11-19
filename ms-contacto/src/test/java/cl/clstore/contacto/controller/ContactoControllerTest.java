package cl.clstore.contacto.controller;

import static cl.clstore.contacto.testsupport.DatosPrueba.contacto;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.contacto.config.SecurityConfig;
import cl.clstore.contacto.dto.ContactoRequest;
import cl.clstore.contacto.exception.RecursoNoEncontradoException;
import cl.clstore.contacto.service.ContactoService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba la capa HTTP: rutas, códigos de estado, validación del cuerpo y formato de los errores.
 * El servicio está simulado; la seguridad real (SecurityConfig) está cargada y se autentica con jwt().
 */
@WebMvcTest(ContactoController.class)
@Import(SecurityConfig.class)
class ContactoControllerTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Administrador");

    private static final String CUERPO_VALIDO = """
        {"nombre":"Ana Pérez","email":"ana@gmail.com","mensaje":"Hola, tengo una consulta."}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ContactoService servicio;

    // --------------------------------------------------------------------- POST

    @Test
    void crear_cuerpoValido_devuelve201ConElMensajeCreado() throws Exception {
        when(servicio.crear(any(ContactoRequest.class))).thenReturn(contacto());

        mvc.perform(post("/api/contactos").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.email").value("ana@gmail.com"))
            .andExpect(jsonPath("$.mensaje").value("Hola, tengo una consulta."))
            .andExpect(jsonPath("$.creado_en").value("2026-01-15T12:00:00Z"));   // nombre JSON "creado_en"

        ArgumentCaptor<ContactoRequest> captor = ArgumentCaptor.forClass(ContactoRequest.class);
        verify(servicio).crear(captor.capture());
        assertEquals("Ana Pérez", captor.getValue().nombre());
        assertEquals("ana@gmail.com", captor.getValue().email());
        assertEquals("Hola, tengo una consulta.", captor.getValue().mensaje());
    }

    @Test
    void crear_camposInvalidos_devuelve400ConErroresPorCampo() throws Exception {
        String cuerpo = "{\"nombre\":\"\",\"email\":\"no-es-correo\",\"mensaje\":\"\"}";

        mvc.perform(post("/api/contactos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.nombre").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.email").value("no es un correo válido"))
            .andExpect(jsonPath("$.errores.mensaje").value("es obligatorio"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_cuerpoSinCampos_devuelve400() throws Exception {
        mvc.perform(post("/api/contactos").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.nombre").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.email").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.mensaje").value("es obligatorio"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_textosDemasiadoLargos_devuelve400() throws Exception {
        String cuerpo = "{\"nombre\":\"" + "a".repeat(101) + "\",\"email\":\"ana@gmail.com\","
            + "\"mensaje\":\"" + "m".repeat(2001) + "\"}";

        mvc.perform(post("/api/contactos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.nombre").value("máximo 100 caracteres"))
            .andExpect(jsonPath("$.errores.mensaje").value("máximo 2000 caracteres"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/contactos").contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es un JSON válido."));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_violacionDeIntegridad_devuelve409() throws Exception {
        when(servicio.crear(any(ContactoRequest.class))).thenThrow(new DataIntegrityViolationException("dato duplicado"));

        mvc.perform(post("/api/contactos").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(containsString("restricción de la base de datos")));
    }

    // ----------------------------------------------------------------------- GET

    @Test
    void listar_devuelve200ConLosMensajes() throws Exception {
        when(servicio.listar()).thenReturn(List.of(
            contacto(2L, "Luis", "luis@gmail.com", "Segundo", Instant.parse("2026-02-01T00:00:00Z")),
            contacto()));

        mvc.perform(get("/api/contactos").with(jwt().authorities(ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].nombre").value("Luis"))
            .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void listar_sinMensajes_devuelveArregloVacio() throws Exception {
        mvc.perform(get("/api/contactos").with(jwt().authorities(ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------------------- DELETE

    @Test
    void eliminar_mensajeExistente_devuelve204() throws Exception {
        mvc.perform(delete("/api/contactos/5").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNoContent());

        verify(servicio).eliminar(5L);
    }

    @Test
    void eliminar_mensajeInexistente_devuelve404ConDetalle() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe el mensaje #9."))
            .when(servicio).eliminar(9L);

        mvc.perform(delete("/api/contactos/9").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No existe el mensaje #9."));
    }

    @Test
    void eliminar_idNoNumerico_devuelve400() throws Exception {
        mvc.perform(delete("/api/contactos/abc").with(jwt().authorities(ADMIN)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }
}
