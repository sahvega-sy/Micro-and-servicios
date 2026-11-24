package cl.clstore.categoria.controller;

import static cl.clstore.categoria.testsupport.DatosPrueba.categoria;
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

import cl.clstore.categoria.config.SecurityConfig;
import cl.clstore.categoria.dto.CategoriaRequest;
import cl.clstore.categoria.exception.ConflictoException;
import cl.clstore.categoria.exception.RecursoNoEncontradoException;
import cl.clstore.categoria.exception.ReglaNegocioException;
import cl.clstore.categoria.exception.ServicioNoDisponibleException;
import cl.clstore.categoria.service.CategoriaService;
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

/**
 * Prueba la capa HTTP: rutas, códigos de estado, validación del cuerpo y formato de los errores.
 * El servicio está simulado; la seguridad real (SecurityConfig) está cargada y se autentica con jwt().
 */
@WebMvcTest(CategoriaController.class)
@Import(SecurityConfig.class)
class CategoriaControllerTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Administrador");

    private static final String CUERPO_VALIDO = "{\"etiqueta\":\"Componentes\",\"icono\":\"fa-microchip\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CategoriaService servicio;

    // ---------------------------------------------------------------- GET lista

    @Test
    void listar_devuelve200ConLasCategorias() throws Exception {
        when(servicio.listar()).thenReturn(List.of(categoria(), categoria("redes", "Redes", "fa-wifi")));

        mvc.perform(get("/api/categorias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].valor").value("componentes"))
            .andExpect(jsonPath("$[0].etiqueta").value("Componentes"))
            .andExpect(jsonPath("$[0].icono").value("fa-microchip"))
            .andExpect(jsonPath("$[1].valor").value("redes"));
    }

    @Test
    void listar_sinCategorias_devuelveArregloVacio() throws Exception {
        mvc.perform(get("/api/categorias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    // ------------------------------------------------------------- GET {valor}

    @Test
    void obtener_categoriaExistente_devuelve200() throws Exception {
        when(servicio.obtener("componentes")).thenReturn(categoria());

        mvc.perform(get("/api/categorias/componentes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valor").value("componentes"))
            .andExpect(jsonPath("$.etiqueta").value("Componentes"));
    }

    @Test
    void obtener_categoriaInexistente_devuelve404ConDetalle() throws Exception {
        when(servicio.obtener("fantasma"))
            .thenThrow(new RecursoNoEncontradoException("No existe la categoría 'fantasma'."));

        mvc.perform(get("/api/categorias/fantasma"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("No existe la categoría 'fantasma'."));
    }

    // --------------------------------------------------------------------- POST

    @Test
    void crear_cuerpoValido_devuelve201YPasaLosDatosAlServicio() throws Exception {
        when(servicio.crear(any(CategoriaRequest.class))).thenReturn(categoria());

        mvc.perform(post("/api/categorias").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.valor").value("componentes"));

        ArgumentCaptor<CategoriaRequest> captor = ArgumentCaptor.forClass(CategoriaRequest.class);
        verify(servicio).crear(captor.capture());
        assertEquals("Componentes", captor.getValue().etiqueta());
        assertEquals("fa-microchip", captor.getValue().icono());
    }

    @Test
    void crear_soloConEtiqueta_elIconoEsOpcional() throws Exception {
        when(servicio.crear(any(CategoriaRequest.class))).thenReturn(categoria("redes", "Redes", "fa-folder"));

        mvc.perform(post("/api/categorias").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"etiqueta\":\"Redes\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.icono").value("fa-folder"));
    }

    @Test
    void crear_etiquetaEnBlanco_devuelve400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/categorias").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"etiqueta\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Datos inválidos")))
            .andExpect(jsonPath("$.errores.etiqueta").value("es obligatoria"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_sinCuerpoUtil_devuelve400() throws Exception {
        mvc.perform(post("/api/categorias").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.etiqueta").value("es obligatoria"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_textosDemasiadoLargos_devuelve400() throws Exception {
        String cuerpo = "{\"etiqueta\":\"" + "e".repeat(101) + "\",\"icono\":\"" + "i".repeat(51) + "\"}";

        mvc.perform(post("/api/categorias").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.etiqueta").value("máximo 100 caracteres"))
            .andExpect(jsonPath("$.errores.icono").value("máximo 50 caracteres"));

        verifyNoInteractions(servicio);
    }

    @Test
    void crear_jsonMalformado_devuelve400() throws Exception {
        mvc.perform(post("/api/categorias").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es un JSON válido."));
    }

    @Test
    void crear_categoriaRepetida_devuelve409() throws Exception {
        when(servicio.crear(any(CategoriaRequest.class))).thenThrow(new ConflictoException("Esa categoría ya existe."));

        mvc.perform(post("/api/categorias").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Esa categoría ya existe."));
    }

    @Test
    void crear_nombreInvalido_devuelve400DeReglaDeNegocio() throws Exception {
        when(servicio.crear(any(CategoriaRequest.class)))
            .thenThrow(new ReglaNegocioException("Ingresa un nombre válido para la categoría."));

        mvc.perform(post("/api/categorias").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"etiqueta\":\"¡¡¡\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Ingresa un nombre válido para la categoría."));
    }

    // ---------------------------------------------------------------------- PUT

    @Test
    void actualizar_cuerpoValido_devuelve200() throws Exception {
        when(servicio.actualizar(eq("componentes"), any(CategoriaRequest.class))).thenReturn(categoria());

        mvc.perform(put("/api/categorias/componentes").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valor").value("componentes"));
    }

    @Test
    void actualizar_categoriaInexistente_devuelve404() throws Exception {
        when(servicio.actualizar(eq("fantasma"), any(CategoriaRequest.class)))
            .thenThrow(new RecursoNoEncontradoException("No existe la categoría 'fantasma'."));

        mvc.perform(put("/api/categorias/fantasma").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
            .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_cuerpoInvalido_devuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(put("/api/categorias/componentes").with(jwt().authorities(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"etiqueta\":\"\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    // ------------------------------------------------------------------- DELETE

    @Test
    void eliminar_categoriaExistente_devuelve204() throws Exception {
        mvc.perform(delete("/api/categorias/redes").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNoContent());

        verify(servicio).eliminar("redes");
    }

    @Test
    void eliminar_categoriaInexistente_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe la categoría 'fantasma'."))
            .when(servicio).eliminar("fantasma");

        mvc.perform(delete("/api/categorias/fantasma").with(jwt().authorities(ADMIN)))
            .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_categoriaConProductos_devuelve409() throws Exception {
        doThrow(new ConflictoException("No se puede eliminar la categoría porque tiene productos asociados."))
            .when(servicio).eliminar("componentes");

        mvc.perform(delete("/api/categorias/componentes").with(jwt().authorities(ADMIN)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(containsString("productos asociados")));
    }

    @Test
    void eliminar_msProductosCaido_devuelve503() throws Exception {
        doThrow(new ServicioNoDisponibleException("ms-productos no está disponible."))
            .when(servicio).eliminar("redes");

        mvc.perform(delete("/api/categorias/redes").with(jwt().authorities(ADMIN)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value("ms-productos no está disponible."));
    }
}
