package cl.clstore.productos.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.productos.controller.ImagenController;
import cl.clstore.productos.controller.ProductoController;
import cl.clstore.productos.controller.StockController;
import cl.clstore.productos.dto.ProductoRequest;
import cl.clstore.productos.service.AlmacenImagenes;
import cl.clstore.productos.service.ProductoService;
import cl.clstore.productos.service.StockService;
import cl.clstore.productos.testsupport.DatosPrueba;
import cl.clstore.productos.testsupport.TokenDePrueba;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

/**
 * Prueba las reglas de acceso con JWT HS256 reales (firmados con el mismo secreto que valida el servicio),
 * por lo que también verifica la conversión del claim "rol" a autoridad.
 */
@WebMvcTest(controllers = {ProductoController.class, StockController.class, ImagenController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String PRODUCTO_JSON =
        "{\"id\":\"CPU-001\",\"nombre\":\"Ryzen\",\"precio\":1,\"stock\":1,\"categoria\":\"componentes\"}";
    private static final String STOCK_JSON = "{\"items\":[{\"id\":\"CPU-001\",\"cantidad\":1}]}";

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private ProductoService productoService;
    @MockitoBean
    private StockService stockService;
    @MockitoBean
    private AlmacenImagenes almacen;

    @BeforeEach
    void preparar() {
        when(productoService.obtener(anyString())).thenReturn(DatosPrueba.producto());
        when(productoService.crear(any(ProductoRequest.class))).thenReturn(DatosPrueba.producto());
        when(almacen.guardar(any(MultipartFile.class))).thenReturn("http://localhost/uploads/productos/x.png");
    }

    private String bearer(String rol) {
        return "Bearer " + TokenDePrueba.generar(secreto, rol, Duration.ofMinutes(5));
    }

    private static MockMultipartFile imagen() {
        return new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1});
    }

    // ------------------------------------------------------------ rutas públicas

    @Test
    void getListado_esPublico() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
    }

    @Test
    void getPorId_esPublico() throws Exception {
        mvc.perform(get("/api/productos/CPU-001")).andExpect(status().isOk());
    }

    // ---------------------------------------------------- escritura de productos

    @Test
    void crear_sinToken_devuelve401() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Administrador,201", "Vendedor,201", "Servicio,403", "Cliente,403"})
    void crear_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_JSON))
            .andExpect(status().is(esperado));
    }

    @ParameterizedTest
    @CsvSource({"Administrador,204", "Vendedor,204", "Servicio,403"})
    void eliminar_segunElRol(String rol, int esperado) throws Exception {
        mvc.perform(delete("/api/productos/CPU-001").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
            .andExpect(status().is(esperado));
    }

    @Test
    void eliminar_sinToken_devuelve401() throws Exception {
        mvc.perform(delete("/api/productos/CPU-001")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ stock (interno)

    @Test
    void descontarStock_sinToken_devuelve401() throws Exception {
        mvc.perform(post("/api/productos/stock/descontar")
                .contentType(MediaType.APPLICATION_JSON).content(STOCK_JSON))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"Servicio,204", "Administrador,403", "Vendedor,403", "Cliente,403"})
    void descontarStock_soloConRolServicio(String rol, int esperado) throws Exception {
        mvc.perform(post("/api/productos/stock/descontar").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(STOCK_JSON))
            .andExpect(status().is(esperado));
    }

    @ParameterizedTest
    @CsvSource({"Servicio,204", "Administrador,403"})
    void reponerStock_soloConRolServicio(String rol, int esperado) throws Exception {
        mvc.perform(post("/api/productos/stock/reponer").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                .contentType(MediaType.APPLICATION_JSON).content(STOCK_JSON))
            .andExpect(status().is(esperado));
    }

    // ------------------------------------------------------------------ imágenes

    @Test
    void subirImagen_sinToken_devuelve401() throws Exception {
        mvc.perform(multipart("/api/productos/imagenes").file(imagen()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void subirImagen_conVendedor_devuelve201() throws Exception {
        mvc.perform(multipart("/api/productos/imagenes").file(imagen())
                .header(HttpHeaders.AUTHORIZATION, bearer("Vendedor")))
            .andExpect(status().isCreated());
    }

    // ---------------------------------------------------------- tokens defectuosos

    @Test
    void token_firmadoConOtroSecreto_devuelve401() throws Exception {
        String falso = TokenDePrueba.generar("otro-secreto-distinto-de-mas-de-32-caracteres",
            "Administrador", Duration.ofMinutes(5));

        mvc.perform(delete("/api/productos/CPU-001").header(HttpHeaders.AUTHORIZATION, "Bearer " + falso))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_expirado_devuelve401() throws Exception {
        String expirado = TokenDePrueba.generar(secreto, "Administrador", Duration.ofMinutes(-10));

        mvc.perform(delete("/api/productos/CPU-001").header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void token_sinClaimRol_devuelve403() throws Exception {
        String sinRol = TokenDePrueba.generar(secreto, null, Duration.ofMinutes(5));

        mvc.perform(delete("/api/productos/CPU-001").header(HttpHeaders.AUTHORIZATION, "Bearer " + sinRol))
            .andExpect(status().isForbidden());
    }

    @Test
    void token_basura_devuelve401() throws Exception {
        mvc.perform(delete("/api/productos/CPU-001").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------- CORS

    @Test
    void cors_origenPermitido_respondeElPreflight() throws Exception {
        mvc.perform(options("/api/productos")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void cors_origenNoPermitido_esRechazado() throws Exception {
        mvc.perform(options("/api/productos")
                .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }
}
