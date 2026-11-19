package cl.clstore.resenas.controller;

import static cl.clstore.resenas.testsupport.DatosPrueba.resena;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import cl.clstore.resenas.config.SecurityConfig;
import cl.clstore.resenas.dto.ResenaRequest;
import cl.clstore.resenas.exception.RecursoNoEncontradoException;
import cl.clstore.resenas.exception.ReglaNegocioException;
import cl.clstore.resenas.exception.ServicioNoDisponibleException;
import cl.clstore.resenas.service.ResenaService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Prueba la capa HTTP: rutas, códigos de estado, validación del cuerpo y formato de los errores.
 * El servicio está simulado; la seguridad real (SecurityConfig) está cargada y se autentica con jwt().
 */
@WebMvcTest(ResenaController.class)
@Import(SecurityConfig.class)
class ResenaControllerTest {

    private static final SimpleGrantedAuthority CLIENTE = new SimpleGrantedAuthority("ROLE_Cliente");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Administrador");

    private static final String CUERPO_VALIDO =
        "{\"producto_id\":\"CPU-001\",\"calificacion\":5,\"comentario\":\"Excelente procesador.\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ResenaService servicio;

    private static RequestPostProcessor sesionDe(String nombre) {
        return jwt().jwt(j -> j.claim("nombre", nombre)).authorities(CLIENTE);
    }

    // ---------------------------------------------------------------- GET lista

    @Test
    void listar_devuelve200ConLasResenasDelProducto() throws Exception {
        when(servicio.listarDeProducto("CPU-001")).thenReturn(List.of(
            resena(), resena(2L, "CPU-001", "Luis", 3, "Normal", LocalDate.of(2026, 1, 5))));

        mvc.perform(get("/api/resenas").param("producto_id", "CPU-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].producto_id").value("CPU-001"))      // nombre JSON heredado del front
            .andExpect(jsonPath("$[0].usuario").value("Ana Pérez"))
            .andExpect(jsonPath("$[0].calificacion").value(5))
            .andExpect(jsonPath("$[0].comentario").value("Excelente procesador."))
            .andExpect(jsonPath("$[0].fecha").value("2026-03-10"))
            .andExpect(jsonPath("$[1].usuario").value("Luis"));

        verify(servicio).listarDeProducto("CPU-001");
    }

    @Test
    void listar_productoSinResenas_devuelveArregloVacio() throws Exception {
        mvc.perform(get("/api/resenas").param("producto_id", "NUEVO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listar_sinElParametroProductoId_devuelve400() throws Exception {
        mvc.perform(get("/api/resenas")).andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    // --------------------------------------------------------------------- POST

    @Test
    void crear_cuerpoValido_devuelve201YUsaElNombreDelToken() throws Exception {
        when(servicio.crear(any(ResenaRequest.class), anyString())).thenReturn(resena());

        mvc.perform(post("/api/resenas").with(sesionDe("Ana Pérez"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuario").value("Ana Pérez"))
            .andExpect(jsonPath("$.calificacion").value(5));

        ArgumentCaptor<ResenaRequest> request = ArgumentCaptor.forClass(ResenaRequest.class);
        ArgumentCaptor<String> autor = ArgumentCaptor.forClass(String.class);
        verify(servicio).crear(request.capture(), autor.capture());
        assertEquals("CPU-001", request.getValue().productoId());          // "producto_id" -> productoId
        assertEquals(5, request.getValue().calificacion());
        assertEquals("Excelente procesador.", request.getValue().comentario());
        assertEquals("Ana Pérez", autor.getValue());
    }

    @Test
    void crear_tokenSinClaimNombre_usaUsuarioComoAutor() throws Exception {
        when(servicio.crear(any(ResenaRequest.class), anyString())).thenReturn(resena());

        mvc.perform(post("/api/resenas").with(jwt().authorities(CLIENTE))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated());

        ArgumentCaptor<String> autor = ArgumentCaptor.forClass(String.class);
        verify(servicio).crear(any(ResenaRequest.class), autor.capture());
        assertEquals("Usuario", autor.getValue());
    }

    @Test
    void crear_claimNombreEnBlanco_usaUsuarioComoAutor() throws Exception {
        when(servicio.crear(any(ResenaRequest.class), anyString())).thenReturn(resena());

        mvc.perform(post("/api/resenas").with(sesionDe("   "))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated());

        ArgumentCaptor<String> autor = ArgumentCaptor.forClass(String.class);
        verify(servicio).crear(any(ResenaRequest.class), autor.capture());
        assertEquals("Usuario", autor.getValue());
    }

    @Test
    void crear_camposInvalidos_devuelve400ConErroresPorCampo() throws Exception {
        String cuerpo = "{\"producto_id\":\"\",\"calificacion\":0,\"comentario\":\"\"}";

        mvc.perform(post("/api/resenas").with(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.productoId").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.calificacion").value("debe ser entre 1 y 5"))
            .andExpect(jsonPath("$.errores.comentario").value("es obligatorio"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_calificacionSobreCinco_devuelve400() throws Exception {
        String cuerpo = "{\"producto_id\":\"CPU-001\",\"calificacion\":6,\"comentario\":\"Ok\"}";

        mvc.perform(post("/api/resenas").with(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.calificacion").value("debe ser entre 1 y 5"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_sinCalificacion_devuelve400() throws Exception {
        String cuerpo = "{\"producto_id\":\"CPU-001\",\"comentario\":\"Ok\"}";

        mvc.perform(post("/api/resenas").with(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.calificacion").value("debe ser entre 1 y 5"));
    }

    @Test
    void crear_comentarioDeMasDeMilCaracteres_devuelve400() throws Exception {
        String cuerpo = "{\"producto_id\":\"CPU-001\",\"calificacion\":4,\"comentario\":\"" + "c".repeat(1001) + "\"}";

        mvc.perform(post("/api/resenas").with(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.comentario").value("máximo 1000 caracteres"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/resenas").with(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es un JSON válido."));
    }

    @Test
    void crear_productoInexistente_devuelve400() throws Exception {
        when(servicio.crear(any(ResenaRequest.class), anyString()))
            .thenThrow(new ReglaNegocioException("El producto 'CPU-001' no existe."));

        mvc.perform(post("/api/resenas").with(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El producto 'CPU-001' no existe."));
    }

    @Test
    void crear_msProductosCaido_devuelve503() throws Exception {
        when(servicio.crear(any(ResenaRequest.class), anyString()))
            .thenThrow(new ServicioNoDisponibleException("No se pudo validar el producto: ms-productos no está disponible."));

        mvc.perform(post("/api/resenas").with(sesionDe("Ana"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isServiceUnavailable());
    }

    // ------------------------------------------------------------------- DELETE

    @Test
    void eliminar_resenaExistente_devuelve204() throws Exception {
        mvc.perform(delete("/api/resenas/7").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNoContent());

        verify(servicio).eliminar(7L);
    }

    @Test
    void eliminar_resenaInexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe la reseña #9."))
            .when(servicio).eliminar(9L);

        mvc.perform(delete("/api/resenas/9").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No existe la reseña #9."));
    }
}
