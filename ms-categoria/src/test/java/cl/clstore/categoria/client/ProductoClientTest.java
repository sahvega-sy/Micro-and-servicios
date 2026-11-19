package cl.clstore.categoria.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.clstore.categoria.exception.ServicioNoDisponibleException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Levanta un "ms-productos" falso con el HttpServer del JDK (sin dependencias extra) y prueba el cliente real. */
class ProductoClientTest {

    private HttpServer servidor;
    private boolean detenido;
    private ProductoClient cliente;
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private final AtomicReference<String> consultaRecibida = new AtomicReference<>();

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

    @Test
    void existenProductosEnCategoria_conProductos_devuelveTrue() {
        arrancarRespondiendo(200, "[{\"id\":\"CPU-001\",\"nombre\":\"Ryzen\"}]");

        assertTrue(cliente.existenProductosEnCategoria("componentes"));
    }

    @Test
    void existenProductosEnCategoria_listaVacia_devuelveFalse() {
        arrancarRespondiendo(200, "[]");

        assertFalse(cliente.existenProductosEnCategoria("redes"));
    }

    @Test
    void existenProductosEnCategoria_respuestaSinCuerpo_devuelveFalse() {
        arrancarRespondiendo(200, null);

        assertFalse(cliente.existenProductosEnCategoria("redes"));
    }

    @Test
    void existenProductosEnCategoria_consultaLaRutaYLosParametrosCorrectos() {
        arrancarRespondiendo(200, "[]");

        cliente.existenProductosEnCategoria("componentes");

        assertEquals("/api/productos", rutaRecibida.get());
        assertTrue(consultaRecibida.get().contains("categoria=componentes"), consultaRecibida.get());
        assertTrue(consultaRecibida.get().contains("limit=1"), consultaRecibida.get());   // solo necesita saber si hay alguno
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 500, 503})
    void existenProductosEnCategoria_errorHttp_lanzaServicioNoDisponible(int status) {
        arrancarRespondiendo(status, null);

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
            () -> cliente.existenProductosEnCategoria("componentes"));

        assertTrue(ex.getMessage().contains("ms-productos no está disponible"));
    }

    @Test
    void existenProductosEnCategoria_msProductosCaido_lanzaServicioNoDisponible() {
        arrancarRespondiendo(200, "[]");
        detener();   // el puerto queda cerrado: conexión rechazada

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.existenProductosEnCategoria("componentes"));
    }

    @Test
    @Timeout(15)
    void existenProductosEnCategoria_msProductosNoResponde_aplicaTimeoutYLanzaServicioNoDisponible() {
        servidor.createContext("/", ex -> {
            try {
                Thread.sleep(5000);   // más que el timeout de lectura (3 s)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ex.close();
        });
        arrancar();

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.existenProductosEnCategoria("componentes"));
    }
}
