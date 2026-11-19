package cl.clstore.ordenes.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.clstore.ordenes.client.ProductoClient.ItemStock;
import cl.clstore.ordenes.dto.ProductoResumen;
import cl.clstore.ordenes.exception.ServicioNoDisponibleException;
import cl.clstore.ordenes.exception.StockInsuficienteException;
import cl.clstore.ordenes.service.TokenServicio;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Levanta un "ms-productos" falso con el HttpServer del JDK (sin dependencias extra) y prueba el cliente real,
 * con un TokenServicio real: así se verifica también el JWT de rol "Servicio" que viaja en cada llamada interna.
 */
class ProductoClientTest {

    private static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String PRODUCTOS_JSON = """
        [{"id":"CPU-001","nombre":"Procesador Ryzen 5","precio":129990,"imagen":"/img/cpu.png",
          "descuento_porcentaje":15,"stock":10,"campo_que_no_interesa":"se ignora"},
         {"id":"RAM-002","nombre":"Memoria 16GB","precio":59990,"imagen":null,
          "descuento_porcentaje":null,"stock":4}]""";

    private HttpServer servidor;
    private boolean detenido;
    private ProductoClient cliente;
    private final AtomicReference<String> metodo = new AtomicReference<>();
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> consulta = new AtomicReference<>();
    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();

    @BeforeEach
    void crearServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    @AfterEach
    void detenerServidor() {
        detener();
    }

    private void detener() {
        if (!detenido) {
            detenido = true;
            servidor.stop(0);
        }
    }

    /** Responde siempre con ese estado; si cuerpo es null responde sin cuerpo. */
    private void arrancarRespondiendo(int status, String contentType, String cuerpo) {
        servidor.createContext("/", ex -> {
            metodo.set(ex.getRequestMethod());
            ruta.set(ex.getRequestURI().getPath());
            consulta.set(ex.getRequestURI().getRawQuery());
            autorizacion.set(ex.getRequestHeaders().getFirst("Authorization"));
            cuerpoRecibido.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (cuerpo == null) {
                ex.sendResponseHeaders(status, -1);
            } else {
                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", contentType);
                ex.sendResponseHeaders(status, bytes.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(bytes);
                }
            }
            ex.close();
        });
        arrancar();
    }

    private void arrancarRespondiendo(int status) {
        arrancarRespondiendo(status, null, null);
    }

    private void arrancar() {
        servidor.start();
        cliente = new ProductoClient("http://127.0.0.1:" + servidor.getAddress().getPort(), new TokenServicio(SECRETO));
    }

    private void assertTokenDeServicioValido() {
        assertTrue(autorizacion.get().startsWith("Bearer "), "debe enviar Authorization: Bearer <jwt>");
        Jwt jwt = NimbusJwtDecoder
            .withSecretKey(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256).build()
            .decode(autorizacion.get().substring("Bearer ".length()));
        assertEquals("Servicio", jwt.getClaimAsString("rol"));
    }

    private static final List<ItemStock> ITEMS = List.of(new ItemStock("CPU-001", 2), new ItemStock("RAM-002", 1));

    // ------------------------------------------------------------ obtenerPorIds

    @Test
    void obtenerPorIds_respuestaValida_mapeaLosProductosEIgnoraCamposDesconocidos() {
        arrancarRespondiendo(200, "application/json", PRODUCTOS_JSON);

        List<ProductoResumen> resultado = cliente.obtenerPorIds(List.of("CPU-001", "RAM-002"));

        assertEquals(2, resultado.size());
        assertEquals("CPU-001", resultado.get(0).id());
        assertEquals("Procesador Ryzen 5", resultado.get(0).nombre());
        assertEquals(new BigDecimal("129990"), resultado.get(0).precio());
        assertEquals(15, resultado.get(0).descuentoPorcentaje());             // "descuento_porcentaje" -> descuentoPorcentaje
        assertEquals(10, resultado.get(0).stock());
        assertNull(resultado.get(1).descuentoPorcentaje());
    }

    @Test
    void obtenerPorIds_esUnaLecturaPublica_conGetYSinToken() {
        arrancarRespondiendo(200, "application/json", "[]");

        cliente.obtenerPorIds(List.of("CPU-001", "RAM-002"));

        assertEquals("GET", metodo.get());
        assertEquals("/api/productos", ruta.get());
        assertTrue(consulta.get().contains("ids=CPU-001"), consulta.get());
        assertTrue(consulta.get().contains("ids=RAM-002"), consulta.get());
        assertNull(autorizacion.get());
    }

    @Test
    void obtenerPorIds_respuestaSinCuerpo_devuelveListaVacia() {
        arrancarRespondiendo(200);

        assertEquals(List.of(), cliente.obtenerPorIds(List.of("CPU-001")));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 500, 503})
    void obtenerPorIds_errorHttp_lanzaServicioNoDisponible(int status) {
        arrancarRespondiendo(status);

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
            () -> cliente.obtenerPorIds(List.of("CPU-001")));

        assertEquals("No se pudo verificar el stock: ms-productos no está disponible.", ex.getMessage());
    }

    @Test
    void obtenerPorIds_msProductosCaido_lanzaServicioNoDisponible() {
        arrancarRespondiendo(200, "application/json", "[]");
        detener();   // el puerto queda cerrado: conexión rechazada

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerPorIds(List.of("CPU-001")));
    }

    @Test
    @Timeout(20)
    void obtenerPorIds_msProductosNoResponde_aplicaTimeoutYLanzaServicioNoDisponible() {
        servidor.createContext("/", ex -> {
            try {
                Thread.sleep(7000);   // más que el timeout de lectura (5 s)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ex.close();
        });
        servidor.start();
        cliente = new ProductoClient("http://127.0.0.1:" + servidor.getAddress().getPort(), new TokenServicio(SECRETO));

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerPorIds(List.of("CPU-001")));
    }

    // ------------------------------------------------------------ descontarStock

    @Test
    void descontarStock_exito_haceUnPostConJwtDeServicioYElCuerpoEsperado() throws Exception {
        arrancarRespondiendo(204);

        assertDoesNotThrow(() -> cliente.descontarStock(ITEMS));

        assertEquals("POST", metodo.get());
        assertEquals("/api/productos/stock/descontar", ruta.get());
        assertTokenDeServicioValido();
        assertEquals(JSON.readTree("{\"items\":[{\"id\":\"CPU-001\",\"cantidad\":2},{\"id\":\"RAM-002\",\"cantidad\":1}]}"),
            JSON.readTree(cuerpoRecibido.get()));
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 409})
    void descontarStock_conflictoONoEncontrado_lanzaStockInsuficienteConElDetalleDeMsProductos(int status) {
        arrancarRespondiendo(status, "application/problem+json",
            "{\"detail\":\"Stock insuficiente para 'Mouse': disponible 1, solicitado 5.\"}");

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
            () -> cliente.descontarStock(ITEMS));

        assertEquals("Stock insuficiente para 'Mouse': disponible 1, solicitado 5.", ex.getMessage());
    }

    @Test
    void descontarStock_conflictoConCuerpoNoJson_usaElMensajeGenerico() {
        arrancarRespondiendo(409, "text/plain", "esto no es json");

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
            () -> cliente.descontarStock(ITEMS));

        assertEquals("No hay stock suficiente para completar la compra.", ex.getMessage());
    }

    @Test
    void descontarStock_conflictoSinCampoDetail_usaElMensajeGenerico() {
        arrancarRespondiendo(409, "application/json", "{}");

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
            () -> cliente.descontarStock(ITEMS));

        assertEquals("No hay stock suficiente para completar la compra.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 500, 503})
    void descontarStock_otrosErroresHttp_lanzaServicioNoDisponible(int status) {
        arrancarRespondiendo(status);

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
            () -> cliente.descontarStock(ITEMS));

        assertEquals("No se pudo reservar el stock: ms-productos no está disponible.", ex.getMessage());
    }

    @Test
    void descontarStock_msProductosCaido_lanzaServicioNoDisponible() {
        arrancarRespondiendo(204);
        detener();

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.descontarStock(ITEMS));
    }

    // -------------------------------------------------------------- reponerStock

    @Test
    void reponerStock_exito_haceUnPostConJwtDeServicioARutaReponer() throws Exception {
        arrancarRespondiendo(204);

        assertDoesNotThrow(() -> cliente.reponerStock(ITEMS));

        assertEquals("POST", metodo.get());
        assertEquals("/api/productos/stock/reponer", ruta.get());
        assertTokenDeServicioValido();
        assertEquals(2, JSON.readTree(cuerpoRecibido.get()).path("items").size());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 404, 409, 500, 503})
    void reponerStock_siMsProductosFalla_noLanzaNada_esMejorEsfuerzo(int status) {
        arrancarRespondiendo(status);

        assertDoesNotThrow(() -> cliente.reponerStock(ITEMS));
    }

    @Test
    void reponerStock_msProductosCaido_noLanzaNada() {
        arrancarRespondiendo(204);
        detener();

        assertDoesNotThrow(() -> cliente.reponerStock(ITEMS));
    }
}
