package cl.clstore.ordenes.controller;

import static cl.clstore.ordenes.testsupport.DatosPrueba.USUARIO;
import static cl.clstore.ordenes.testsupport.DatosPrueba.ordenResponse;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.ordenes.config.SecurityConfig;
import cl.clstore.ordenes.dto.CrearOrdenResponse;
import cl.clstore.ordenes.dto.ItemOrdenResponse;
import cl.clstore.ordenes.dto.OrdenRequest;
import cl.clstore.ordenes.dto.OrdenResponse;
import cl.clstore.ordenes.exception.OrdenRechazadaException;
import cl.clstore.ordenes.exception.ProhibidoException;
import cl.clstore.ordenes.exception.RecursoNoEncontradoException;
import cl.clstore.ordenes.exception.ReglaNegocioException;
import cl.clstore.ordenes.exception.ServicioNoDisponibleException;
import cl.clstore.ordenes.service.OrdenService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
 * Prueba la capa HTTP: rutas, códigos de estado, validación del cuerpo y formato de los errores.
 * El servicio está simulado; la seguridad real (SecurityConfig) está cargada y se autentica con jwt().
 */
@WebMvcTest(OrdenController.class)
@Import(SecurityConfig.class)
class OrdenControllerTest {

    private static final String CUERPO_VALIDO = """
        {"nombre":"Ana","apellidos":"Pérez","correo":"ana@gmail.com","telefono":"+56911111111",
         "region":"Región Metropolitana de Santiago","comuna":"Santiago","direccion":"Calle Falsa 123",
         "items":[{"producto_id":"CPU-001","cantidad":2},{"producto_id":"RAM-002","cantidad":1}]}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrdenService servicio;

    /** Sesión con id (claim "sub") USUARIO y el rol indicado. */
    private static RequestPostProcessor sesion(String rol) {
        return jwt().jwt(j -> j.subject(USUARIO).claim("rol", rol))
            .authorities(AuthorityUtils.createAuthorityList("ROLE_" + rol));
    }

    /** Cuerpo válido, salvo el fragmento que se reemplaza. */
    private static String cuerpoCon(String desde, String hasta) {
        return CUERPO_VALIDO.replace(desde, hasta);
    }

    // --------------------------------------------------------------------- POST

    @Test
    void crear_compraDeInvitado_devuelve201YPasaUsuarioNull() throws Exception {
        when(servicio.crear(any(OrdenRequest.class), isNull()))
            .thenReturn(new CrearOrdenResponse(100L, new BigDecimal("280974"), "exitosa"));

        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orden_id").value(100))                 // nombre JSON heredado de la función crear_orden
            .andExpect(jsonPath("$.total").value(280974))
            .andExpect(jsonPath("$.estado").value("exitosa"));

        ArgumentCaptor<OrdenRequest> captor = ArgumentCaptor.forClass(OrdenRequest.class);
        verify(servicio).crear(captor.capture(), isNull());
        assertEquals("Ana", captor.getValue().nombre());
        assertEquals("Región Metropolitana de Santiago", captor.getValue().region());
        assertEquals(2, captor.getValue().items().size());
        assertEquals("CPU-001", captor.getValue().items().get(0).productoId());     // "producto_id" -> productoId
        assertEquals(2, captor.getValue().items().get(0).cantidad());
    }

    @Test
    void crear_conSesion_asociaLaOrdenAlUsuarioDelToken() throws Exception {
        when(servicio.crear(any(OrdenRequest.class), anyString()))
            .thenReturn(new CrearOrdenResponse(101L, new BigDecimal("100"), "exitosa"));

        mvc.perform(post("/api/ordenes").with(sesion("Cliente"))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated());

        verify(servicio).crear(any(OrdenRequest.class), org.mockito.ArgumentMatchers.eq(USUARIO));
    }

    @Test
    void crear_telefonoEsOpcional() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCon("\"telefono\":\"+56911111111\",", "")))
            .andExpect(status().isCreated());
    }

    @Test
    void crear_camposDelCompradorInvalidos_devuelve400ConErroresPorCampo() throws Exception {
        String cuerpo = "{\"nombre\":\"\",\"apellidos\":\"\",\"correo\":\"no-es-correo\",\"region\":\"\","
            + "\"comuna\":\"\",\"direccion\":\"\",\"items\":[{\"producto_id\":\"CPU-001\",\"cantidad\":1}]}";

        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.nombre").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.apellidos").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.correo").value("no es un correo válido"))
            .andExpect(jsonPath("$.errores.region").value("es obligatoria"))
            .andExpect(jsonPath("$.errores.comuna").value("es obligatoria"))
            .andExpect(jsonPath("$.errores.direccion").value("es obligatoria"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_telefonoMuyLargo_devuelve400() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCon("+56911111111", "9".repeat(31))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.telefono").value("máximo 30 caracteres"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_sinItems_devuelve400() throws Exception {
        String cuerpo = "{\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"correo\":\"ana@gmail.com\","
            + "\"region\":\"R\",\"comuna\":\"C\",\"direccion\":\"D\",\"items\":[]}";

        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.items").value("debe incluir al menos un producto"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_lineaConCantidadCero_devuelve400() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCon("\"cantidad\":2", "\"cantidad\":0")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores['items[0].cantidad']").value("debe ser al menos 1"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_lineaConCantidadSobreElMaximo_devuelve400() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCon("\"cantidad\":2", "\"cantidad\":100")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores['items[0].cantidad']").value("máximo 99"));
    }

    @Test
    void crear_lineaSinProductoId_devuelve400() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCon("\"producto_id\":\"CPU-001\"", "\"producto_id\":\" \"")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores['items[0].productoId']").value("es obligatorio"));
    }

    @Test
    void crear_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es un JSON válido."));
    }

    @Test
    void crear_ordenRechazada_devuelve409ConElDetalleYElIdDeLaOrdenFallida() throws Exception {
        when(servicio.crear(any(OrdenRequest.class), isNull()))
            .thenThrow(new OrdenRechazadaException("Stock insuficiente para 'Mouse': disponible 1, solicitado 5.", 55L));

        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(containsString("Stock insuficiente")))
            .andExpect(jsonPath("$.orden_id").value(55));
    }

    @Test
    void crear_msProductosCaido_devuelve503() throws Exception {
        when(servicio.crear(any(OrdenRequest.class), isNull()))
            .thenThrow(new ServicioNoDisponibleException("No se pudo reservar el stock: ms-productos no está disponible."));

        mvc.perform(post("/api/ordenes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isServiceUnavailable());
    }

    // ----------------------------------------------------------------------- GET

    @Test
    void listar_sinFiltro_devuelve200SinItemsEnCadaOrden() throws Exception {
        when(servicio.listar(null)).thenReturn(List.of(ordenResponse()));

        mvc.perform(get("/api/ordenes").with(sesion("Administrador")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(5))
            .andExpect(jsonPath("$[0].usuario_id").value(USUARIO))             // nombres JSON heredados de Supabase
            .andExpect(jsonPath("$[0].nombre_comprador").value("Ana"))
            .andExpect(jsonPath("$[0].apellidos_comprador").value("Pérez"))
            .andExpect(jsonPath("$[0].correo_comprador").value("ana@gmail.com"))
            .andExpect(jsonPath("$[0].total").value(280974))
            .andExpect(jsonPath("$[0].estado").value("exitosa"))
            .andExpect(jsonPath("$[0].creado_en").value("2026-01-15T12:00:00Z"))
            .andExpect(jsonPath("$[0].items").doesNotExist());                // NON_NULL: el listado no trae líneas
    }

    @Test
    void listar_conEstado_loPasaAlServicio() throws Exception {
        mvc.perform(get("/api/ordenes").param("estado", "fallida").with(sesion("Vendedor")))
            .andExpect(status().isOk());

        verify(servicio).listar("fallida");
    }

    @Test
    void listar_estadoInvalido_devuelve400() throws Exception {
        when(servicio.listar("raro")).thenThrow(new ReglaNegocioException("El estado debe ser 'exitosa' o 'fallida'."));

        mvc.perform(get("/api/ordenes").param("estado", "raro").with(sesion("Administrador")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El estado debe ser 'exitosa' o 'fallida'."));
    }

    @Test
    void items_devuelveTodasLasLineasParaLosReportes() throws Exception {
        when(servicio.todosLosItems()).thenReturn(List.of(
            new ItemOrdenResponse(1L, 5L, "CPU-001", "Procesador Ryzen 5", 2, new BigDecimal("110492"))));

        mvc.perform(get("/api/ordenes/items").with(sesion("Administrador")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].orden_id").value(5))
            .andExpect(jsonPath("$[0].producto_id").value("CPU-001"))
            .andExpect(jsonPath("$[0].nombre_producto").value("Procesador Ryzen 5"))
            .andExpect(jsonPath("$[0].cantidad").value(2))
            .andExpect(jsonPath("$[0].precio_unitario").value(110492));
    }

    @Test
    void mias_devuelveLasOrdenesDelUsuarioDelToken() throws Exception {
        when(servicio.listarDeUsuario(USUARIO)).thenReturn(List.of(ordenResponse()));

        mvc.perform(get("/api/ordenes/mias").with(sesion("Cliente")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(5));

        verify(servicio).listarDeUsuario(USUARIO);
    }

    // ----------------------------------------------------------------- GET {id}

    @ParameterizedTest
    @CsvSource({"Administrador,true", "Vendedor,true", "Cliente,false", "Servicio,false"})
    void obtener_indicaAlServicioSiElUsuarioEsPersonal(String rol, boolean esPersonal) throws Exception {
        when(servicio.obtener(anyLong(), anyString(), anyBoolean())).thenReturn(ordenResponse());

        mvc.perform(get("/api/ordenes/5").with(sesion(rol)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5));

        verify(servicio).obtener(5L, USUARIO, esPersonal);
    }

    @Test
    void obtener_ordenDeOtroUsuario_devuelve403() throws Exception {
        when(servicio.obtener(5L, USUARIO, false)).thenThrow(new ProhibidoException("No tienes acceso a esta orden."));

        mvc.perform(get("/api/ordenes/5").with(sesion("Cliente")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("No tienes acceso a esta orden."));
    }

    @Test
    void obtener_ordenInexistente_devuelve404() throws Exception {
        when(servicio.obtener(9L, USUARIO, true)).thenThrow(new RecursoNoEncontradoException("No existe la orden #9."));

        mvc.perform(get("/api/ordenes/9").with(sesion("Administrador")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No existe la orden #9."));
    }

    @Test
    void obtener_idNoNumerico_devuelve400() throws Exception {
        mvc.perform(get("/api/ordenes/abc").with(sesion("Administrador")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }
}
