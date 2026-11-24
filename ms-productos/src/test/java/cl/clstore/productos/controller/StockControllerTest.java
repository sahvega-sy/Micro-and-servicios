package cl.clstore.productos.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.productos.config.SecurityConfig;
import cl.clstore.productos.exception.ConflictoException;
import cl.clstore.productos.exception.RecursoNoEncontradoException;
import cl.clstore.productos.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Endpoints internos que consume ms-ordenes con un JWT de rol "Servicio". */
@WebMvcTest(StockController.class)
@Import(SecurityConfig.class)
class StockControllerTest {

    private static final SimpleGrantedAuthority SERVICIO = new SimpleGrantedAuthority("ROLE_Servicio");

    private static final String CUERPO_VALIDO = """
        {"items":[{"id":"CPU-001","cantidad":2},{"id":"RAM-002","cantidad":1}]}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StockService servicio;

    @Test
    void descontar_cuerpoValido_devuelve204YPasaLosItems() throws Exception {
        mvc.perform(post("/api/productos/stock/descontar").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isNoContent());

        verify(servicio).descontar(argThat(items ->
            items.size() == 2
                && items.get(0).id().equals("CPU-001") && items.get(0).cantidad() == 2
                && items.get(1).id().equals("RAM-002") && items.get(1).cantidad() == 1));
    }

    @Test
    void descontar_stockInsuficiente_devuelve409ConElDetalle() throws Exception {
        doThrow(new ConflictoException("Stock insuficiente para 'Mouse': disponible 1, solicitado 5."))
            .when(servicio).descontar(anyList());

        mvc.perform(post("/api/productos/stock/descontar").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(containsString("Stock insuficiente")));
    }

    @Test
    void descontar_productoInexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe el producto 'X'."))
            .when(servicio).descontar(anyList());

        mvc.perform(post("/api/productos/stock/descontar").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isNotFound());
    }

    @Test
    void descontar_listaVacia_devuelve400() throws Exception {
        mvc.perform(post("/api/productos/stock/descontar").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.items").value("debe incluir al menos un producto"));

        verifyNoInteractions(servicio);
    }

    @Test
    void descontar_sinItems_devuelve400() throws Exception {
        mvc.perform(post("/api/productos/stock/descontar").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void descontar_cantidadCero_devuelve400() throws Exception {
        String cuerpo = "{\"items\":[{\"id\":\"CPU-001\",\"cantidad\":0}]}";

        mvc.perform(post("/api/productos/stock/descontar").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores['items[0].cantidad']").value("debe ser al menos 1"));

        verifyNoInteractions(servicio);
    }

    @Test
    void descontar_idEnBlanco_devuelve400() throws Exception {
        String cuerpo = "{\"items\":[{\"id\":\" \",\"cantidad\":1}]}";

        mvc.perform(post("/api/productos/stock/descontar").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores['items[0].id']").value("es obligatorio"));
    }

    @Test
    void reponer_cuerpoValido_devuelve204YPasaLosItems() throws Exception {
        mvc.perform(post("/api/productos/stock/reponer").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isNoContent());

        verify(servicio).reponer(argThat(items -> items.size() == 2 && items.get(0).id().equals("CPU-001")));
    }

    @Test
    void reponer_cuerpoInvalido_devuelve400() throws Exception {
        mvc.perform(post("/api/productos/stock/reponer").with(jwt().authorities(SERVICIO))
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }
}
