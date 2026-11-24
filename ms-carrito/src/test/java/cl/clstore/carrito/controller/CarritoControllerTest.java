package cl.clstore.carrito.controller;

import static cl.clstore.carrito.testsupport.DatosPrueba.USUARIO;
import static cl.clstore.carrito.testsupport.DatosPrueba.itemRespuesta;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import cl.clstore.carrito.config.SecurityConfig;
import cl.clstore.carrito.exception.RecursoNoEncontradoException;
import cl.clstore.carrito.exception.ReglaNegocioException;
import cl.clstore.carrito.exception.ServicioNoDisponibleException;
import cl.clstore.carrito.service.CarritoService;
import java.util.List;
import org.junit.jupiter.api.Test;
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
 * El usuario del carrito sale del claim "sub" del token.
 */
@WebMvcTest(CarritoController.class)
@Import(SecurityConfig.class)
class CarritoControllerTest {

    private static final SimpleGrantedAuthority CLIENTE = new SimpleGrantedAuthority("ROLE_Cliente");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CarritoService servicio;

    /** Sesión de un Cliente cuyo id (claim "sub") es USUARIO. */
    private static RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(USUARIO)).authorities(CLIENTE);
    }

    // ---------------------------------------------------------------- GET lista

    @Test
    void listar_devuelve200ConElCarritoDelUsuarioDelToken() throws Exception {
        when(servicio.listar(USUARIO)).thenReturn(List.of(itemRespuesta()));

        mvc.perform(get("/api/carrito").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].producto_id").value("CPU-001"))          // nombre JSON heredado del front
            .andExpect(jsonPath("$[0].cantidad").value(2))
            .andExpect(jsonPath("$[0].productos.id").value("CPU-001"))
            .andExpect(jsonPath("$[0].productos.nombre").value("Procesador Ryzen 5"))
            .andExpect(jsonPath("$[0].productos.precio").value(129990))
            .andExpect(jsonPath("$[0].productos.descuento_porcentaje").value(15))
            .andExpect(jsonPath("$[0].productos.stock").value(10));

        verify(servicio).listar(USUARIO);
    }

    @Test
    void listar_carritoVacio_devuelveArregloVacio() throws Exception {
        mvc.perform(get("/api/carrito").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listar_msProductosCaido_devuelve503() throws Exception {
        when(servicio.listar(USUARIO)).thenThrow(new ServicioNoDisponibleException("ms-productos no está disponible."));

        mvc.perform(get("/api/carrito").with(sesion()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value("ms-productos no está disponible."));
    }

    // ---------------------------------------------------------------- POST items

    @Test
    void agregar_cuerpoValido_devuelve200YPasaLosDatosAlServicio() throws Exception {
        when(servicio.agregar(USUARIO, "CPU-001", 2)).thenReturn(List.of(itemRespuesta()));

        mvc.perform(post("/api/carrito/items").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"producto_id\":\"CPU-001\",\"cantidad\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].producto_id").value("CPU-001"));

        verify(servicio).agregar(USUARIO, "CPU-001", 2);
    }

    @Test
    void agregar_sinCantidad_seEnviaNullAlServicio() throws Exception {
        mvc.perform(post("/api/carrito/items").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"producto_id\":\"CPU-001\"}"))
            .andExpect(status().isOk());

        verify(servicio).agregar(USUARIO, "CPU-001", null);
    }

    @Test
    void agregar_productoIdEnBlanco_devuelve400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/carrito/items").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"producto_id\":\"  \",\"cantidad\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.productoId").value("es obligatorio"));

        verifyNoInteractions(servicio);
    }

    @Test
    void agregar_sinProductoId_devuelve400() throws Exception {
        mvc.perform(post("/api/carrito/items").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.productoId").value("es obligatorio"));

        verifyNoInteractions(servicio);
    }

    @Test
    void agregar_cantidadCero_devuelve400() throws Exception {
        mvc.perform(post("/api/carrito/items").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"producto_id\":\"CPU-001\",\"cantidad\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.cantidad").value("debe ser al menos 1"));

        verifyNoInteractions(servicio);
    }

    @Test
    void agregar_cantidadSobreElMaximo_devuelve400() throws Exception {
        mvc.perform(post("/api/carrito/items").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"producto_id\":\"CPU-001\",\"cantidad\":100}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.cantidad").value("máximo 99"));

        verifyNoInteractions(servicio);
    }

    @Test
    void agregar_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/carrito/items").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es un JSON válido."));
    }

    @Test
    void agregar_productoInexistente_devuelve400() throws Exception {
        when(servicio.agregar(anyString(), anyString(), eq(1)))
            .thenThrow(new ReglaNegocioException("El producto 'FANTASMA' no existe."));

        mvc.perform(post("/api/carrito/items").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"producto_id\":\"FANTASMA\",\"cantidad\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El producto 'FANTASMA' no existe."));
    }

    // ------------------------------------------------------------ PUT items/{id}

    @Test
    void cambiarCantidad_cuerpoValido_devuelve200() throws Exception {
        when(servicio.cambiarCantidad(USUARIO, "CPU-001", 3)).thenReturn(List.of(itemRespuesta()));

        mvc.perform(put("/api/carrito/items/CPU-001").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(servicio).cambiarCantidad(USUARIO, "CPU-001", 3);
    }

    @Test
    void cambiarCantidad_sinCantidad_devuelve400() throws Exception {
        mvc.perform(put("/api/carrito/items/CPU-001").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.cantidad").value("es obligatoria"));

        verifyNoInteractions(servicio);
    }

    @Test
    void cambiarCantidad_cantidadCero_devuelve400() throws Exception {
        mvc.perform(put("/api/carrito/items/CPU-001").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.cantidad").value("debe ser al menos 1"));
    }

    @Test
    void cambiarCantidad_cantidadSobreElMaximo_devuelve400() throws Exception {
        mvc.perform(put("/api/carrito/items/CPU-001").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":100}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.cantidad").value("máximo 99"));
    }

    @Test
    void cambiarCantidad_productoFueraDelCarrito_devuelve404() throws Exception {
        when(servicio.cambiarCantidad(anyString(), anyString(), anyInt()))
            .thenThrow(new RecursoNoEncontradoException("El producto 'CPU-001' no está en tu carrito."));

        mvc.perform(put("/api/carrito/items/CPU-001").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":3}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("El producto 'CPU-001' no está en tu carrito."));
    }

    // ---------------------------------------------------------------- DELETE

    @Test
    void eliminarProducto_devuelve200ConElCarritoRestante() throws Exception {
        when(servicio.eliminar(USUARIO, "CPU-001")).thenReturn(List.of());

        mvc.perform(delete("/api/carrito/items/CPU-001").with(sesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(servicio).eliminar(USUARIO, "CPU-001");
    }

    @Test
    void vaciar_devuelve204() throws Exception {
        mvc.perform(delete("/api/carrito").with(sesion()))
            .andExpect(status().isNoContent());

        verify(servicio).vaciar(USUARIO);
    }
}
