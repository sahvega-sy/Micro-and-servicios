package cl.clstore.carrito.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.clstore.carrito.dto.ProductoResumen;
import cl.clstore.carrito.exception.ServicioNoDisponibleException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Levanta un "ms-productos" falso con el HttpServer del JDK (sin dependencias extra) y prueba el cliente real. */
class ProductoClientTest {

    private static final String PRODUCTOS_JSON = """
        [{"id":"CPU-001","nombre":"Procesador Ryzen 5","precio":129990,"imagen":"/img/cpu.png",
          "descuento_porcentaje":15,"stock":10,"campo_que_no_interesa":"se ignora"},
         {"id":"RAM-002","nombre":"Memoria 16GB","precio":59990,"imagen":null,
          "descuento_porcentaje":null,"stock":4}]""";

    private HttpServer servidor;
    private boolean detenido;
    private ProductoClient cliente;
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private final AtomicReference<String> consultaRecibida = new AtomicReference<>();
    private final AtomicInteger peticiones = new AtomicInteger();

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

    /** Responde siempre con ese estado; si json es null responde sin cuerpo. */
    private void arrancarRespondiendo(int status, String json) {
        servidor.createContext("/", ex -> {
            peticiones.incrementAndGet();
            rutaRecibida.set(ex.getRequestURI().getPath());
            consultaRecibida.set(ex.getRequestURI().getRawQuery());
            if (json == null) {
                ex.sendResponseHeaders(status, -1);
            } else {
                byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(status, cuerpo.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(cuerpo);
                }
            }
            ex.close();
        });
        arrancar();
    }

    private void arrancar() {
        servidor.start();
        cliente = new ProductoClient("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    // ------------------------------------------------------------ obtenerPorIds

    @Test
    void obtenerPorIds_respuestaValida_mapeaLosProductosEIgnoraCamposDesconocidos() {
        arrancarRespondiendo(200, PRODUCTOS_JSON);

        List<ProductoResumen> resultado = cliente.obtenerPorIds(List.of("CPU-001", "RAM-002"));

        assertEquals(2, resultado.size());
        ProductoResumen cpu = resultado.get(0);
        assertEquals("CPU-001", cpu.id());
        assertEquals("Procesador Ryzen 5", cpu.nombre());
        assertEquals(new BigDecimal("129990"), cpu.precio());
        assertEquals("/img/cpu.png", cpu.imagen());
        assertEquals(15, cpu.descuentoPorcentaje());                  // "descuento_porcentaje" -> descuentoPorcentaje
        assertEquals(10, cpu.stock());
        assertNull(resultado.get(1).descuentoPorcentaje());
        assertNull(resultado.get(1).imagen());
    }

    @Test
    void obtenerPorIds_consultaLaRutaYEnviaTodosLosIds() {
        arrancarRespondiendo(200, "[]");

        cliente.obtenerPorIds(List.of("CPU-001", "RAM-002"));

        assertEquals("/api/productos", rutaRecibida.get());
        assertTrue(consultaRecibida.get().contains("ids=CPU-001"), consultaRecibida.get());
        assertTrue(consultaRecibida.get().contains("ids=RAM-002"), consultaRecibida.get());
    }

    @Test
    void obtenerPorIds_sinIds_devuelveListaVaciaSinLlamarAMsProductos() {
        arrancarRespondiendo(200, PRODUCTOS_JSON);

        assertEquals(List.of(), cliente.obtenerPorIds(List.of()));

        assertEquals(0, peticiones.get());
    }

    @Test
    void obtenerPorIds_respuestaSinCuerpo_devuelveListaVacia() {
        arrancarRespondiendo(200, null);

        assertEquals(List.of(), cliente.obtenerPorIds(List.of("CPU-001")));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 500, 503})
    void obtenerPorIds_errorHttp_lanzaServicioNoDisponible(int status) {
        arrancarRespondiendo(status, null);

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
            () -> cliente.obtenerPorIds(List.of("CPU-001")));

        assertEquals("ms-productos no está disponible.", ex.getMessage());
    }

    @Test
    void obtenerPorIds_msProductosCaido_lanzaServicioNoDisponible() {
        arrancarRespondiendo(200, "[]");
        detener();   // el puerto queda cerrado: conexión rechazada

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerPorIds(List.of("CPU-001")));
    }

    @Test
    @Timeout(20)
    void obtenerPorIds_msProductosNoResponde_aplicaTimeoutYLanzaServicioNoDisponible() {
        servidor.createContext("/", ex -> {
            try {
                Thread.sleep(6000);   // más que el timeout de lectura (4 s)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ex.close();
        });
        arrancar();

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.obtenerPorIds(List.of("CPU-001")));
    }

    // ------------------------------------------------------------------- existe

    @Test
    void existe_productoExistente_devuelveTrueYConsultaLaRutaCorrecta() {
        arrancarRespondiendo(200, "{\"id\":\"CPU-001\"}");

        assertTrue(cliente.existe("CPU-001"));

        assertEquals("/api/productos/CPU-001", rutaRecibida.get());
    }

    @Test
    void existe_productoInexistente_devuelveFalse() {
        arrancarRespondiendo(404, null);

        assertFalse(cliente.existe("FANTASMA"));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 500, 503})
    void existe_otrosErroresHttp_lanzaServicioNoDisponible(int status) {
        arrancarRespondiendo(status, null);

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
            () -> cliente.existe("CPU-001"));

        assertEquals("ms-productos no está disponible.", ex.getMessage());
    }

    @Test
    void existe_msProductosCaido_lanzaServicioNoDisponible() {
        arrancarRespondiendo(200, "{}");
        detener();

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.existe("CPU-001"));
    }
}
