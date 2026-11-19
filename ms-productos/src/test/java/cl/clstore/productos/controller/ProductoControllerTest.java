package cl.clstore.productos.controller;

import static cl.clstore.productos.testsupport.DatosPrueba.producto;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

import cl.clstore.productos.config.SecurityConfig;
import cl.clstore.productos.dto.ProductoRequest;
import cl.clstore.productos.exception.ConflictoException;
import cl.clstore.productos.exception.RecursoNoEncontradoException;
import cl.clstore.productos.exception.ReglaNegocioException;
import cl.clstore.productos.exception.ServicioNoDisponibleException;
import cl.clstore.productos.service.ProductoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba la capa HTTP: rutas, códigos de estado, validación del cuerpo y formato de los errores.
 * El servicio está simulado; la seguridad real (SecurityConfig) está cargada y se autentica con jwt().
 */
@WebMvcTest(ProductoController.class)
@Import(SecurityConfig.class)
class ProductoControllerTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Administrador");

    private static final String CUERPO_VALIDO = """
        {"id":"CPU-001","nombre":"Procesador Ryzen 5","descripcion":"6 núcleos","precio":129990,
         "stock":10,"stock_critico":3,"categoria":"componentes"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductoService servicio;

    // ---------------------------------------------------------------- GET lista

    @Test
    void listar_sinFiltros_devuelve200ConLosProductos() throws Exception {
        when(servicio.listar(any(), any(), anyBoolean(), any(), any(), any(), any()))
            .thenReturn(List.of(producto()));

        mvc.perform(get("/api/productos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value("CPU-001"))
            .andExpect(jsonPath("$[0].nombre").value("Procesador Ryzen 5"))
            .andExpect(jsonPath("$[0].stock_critico").value(2));   // nombre JSON heredado del front

        verify(servicio).listar(null, null, false, null, null, null, null);
    }

    @Test
    void listar_conTodosLosFiltros_losPasaAlServicio() throws Exception {
        mvc.perform(get("/api/productos")
                .param("categoria", "componentes")
                .param("q", "ryzen")
                .param("oferta", "true")
                .param("excluir", "CPU-002")
                .param("ids", "a,b")
                .param("orden", "creado_desc")
                .param("limit", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(servicio).listar("componentes", "ryzen", true, "CPU-002", List.of("a", "b"), "creado_desc", 3);
    }

    // ----------------------------------------------------------------- GET {id}

    @Test
    void obtener_productoExistente_devuelve200() throws Exception {
        when(servicio.obtener("CPU-001")).thenReturn(producto());

        mvc.perform(get("/api/productos/CPU-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("CPU-001"))
            .andExpect(jsonPath("$.categoria").value("componentes"));
    }

    @Test
    void obtener_productoInexistente_devuelve404ConDetalle() throws Exception {
        when(servicio.obtener("NO-EXISTE"))
            .thenThrow(new RecursoNoEncontradoException("No existe el producto 'NO-EXISTE'."));

        mvc.perform(get("/api/productos/NO-EXISTE"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No existe el producto 'NO-EXISTE'."));
    }

    // --------------------------------------------------------------------- POST

    @Test
    void crear_cuerpoValido_devuelve201YMapeaLosNombresJson() throws Exception {
        when(servicio.crear(any(ProductoRequest.class))).thenReturn(producto());

        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("CPU-001"));

        ArgumentCaptor<ProductoRequest> captor = ArgumentCaptor.forClass(ProductoRequest.class);
        verify(servicio).crear(captor.capture());
        assertEquals("Procesador Ryzen 5", captor.getValue().nombre());
        assertEquals(3, captor.getValue().stockCritico());          // "stock_critico" -> stockCritico
    }

    @Test
    void crear_camposInvalidos_devuelve400ConErroresPorCampo() throws Exception {
        String cuerpo = "{\"nombre\":\"\",\"precio\":-5,\"stock\":-1,\"categoria\":\"\"}";

        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.nombre").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.precio").value("no puede ser negativo"))
            .andExpect(jsonPath("$.errores.stock").value("no puede ser negativo"))
            .andExpect(jsonPath("$.errores.categoria").value("es obligatoria"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_faltanPrecioYStock_devuelve400() throws Exception {
        String cuerpo = "{\"nombre\":\"Ryzen\",\"categoria\":\"componentes\"}";

        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.precio").value("es obligatorio"))
            .andExpect(jsonPath("$.errores.stock").value("es obligatorio"));
    }

    @Test
    void crear_idMuyCorto_devuelve400() throws Exception {
        String cuerpo = "{\"id\":\"ab\",\"nombre\":\"Ryzen\",\"precio\":1,\"stock\":1,\"categoria\":\"c\"}";

        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.id").value("el código debe tener entre 3 y 100 caracteres"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-3, 0, 96, 100})
    void crear_descuentoFueraDeRango_devuelve400(int descuento) throws Exception {
        String cuerpo = "{\"id\":\"CPU-001\",\"nombre\":\"Ryzen\",\"precio\":1,\"stock\":1,"
            + "\"categoria\":\"c\",\"descuento_porcentaje\":" + descuento + "}";

        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.descuentoPorcentaje").value("debe ser entre 1 y 95"));
    }

    @Test
    void crear_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es un JSON válido."));
    }

    @Test
    void crear_codigoRepetido_devuelve409() throws Exception {
        when(servicio.crear(any(ProductoRequest.class)))
            .thenThrow(new ConflictoException("Ya existe un producto con ese código."));

        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Ya existe un producto con ese código."));
    }

    @Test
    void crear_categoriaInexistente_devuelve400() throws Exception {
        when(servicio.crear(any(ProductoRequest.class)))
            .thenThrow(new ReglaNegocioException("La categoría 'componentes' no existe."));

        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("no existe")));
    }

    @Test
    void crear_msCategoriaCaido_devuelve503() throws Exception {
        when(servicio.crear(any(ProductoRequest.class)))
            .thenThrow(new ServicioNoDisponibleException("ms-categoria no está disponible."));

        mvc.perform(post("/api/productos").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isServiceUnavailable());
    }

    // ---------------------------------------------------------------------- PUT

    @Test
    void actualizar_cuerpoValido_devuelve200() throws Exception {
        when(servicio.actualizar(eq("CPU-001"), any(ProductoRequest.class))).thenReturn(producto());

        mvc.perform(put("/api/productos/CPU-001").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("CPU-001"));
    }

    @Test
    void actualizar_productoInexistente_devuelve404() throws Exception {
        when(servicio.actualizar(eq("NO-EXISTE"), any(ProductoRequest.class)))
            .thenThrow(new RecursoNoEncontradoException("No existe el producto 'NO-EXISTE'."));

        mvc.perform(put("/api/productos/NO-EXISTE").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_cuerpoInvalido_devuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(put("/api/productos/CPU-001").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    // ------------------------------------------------------------------- DELETE

    @Test
    void eliminar_productoExistente_devuelve204() throws Exception {
        mvc.perform(delete("/api/productos/CPU-001").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNoContent());

        verify(servicio).eliminar("CPU-001");
    }

    @Test
    void eliminar_productoInexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe el producto 'NO-EXISTE'."))
            .when(servicio).eliminar("NO-EXISTE");

        mvc.perform(delete("/api/productos/NO-EXISTE").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNotFound());
    }
}
