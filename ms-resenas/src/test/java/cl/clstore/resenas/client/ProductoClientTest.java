package cl.clstore.resenas.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.clstore.resenas.exception.ReglaNegocioException;
import cl.clstore.resenas.exception.ServicioNoDisponibleException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
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

    private void arrancarRespondiendo(int status) {
        servidor.createContext("/", ex -> {
            rutaRecibida.set(ex.getRequestURI().getPath());
            ex.sendResponseHeaders(status, -1);
            ex.close();
        });
        arrancar();
    }

    private void arrancar() {
        servidor.start();
        cliente = new ProductoClient("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @Test
    void validarExiste_productoExistente_noLanzaYConsultaLaRutaCorrecta() {
        arrancarRespondiendo(200);

        assertDoesNotThrow(() -> cliente.validarExiste("CPU-001"));

        assertEquals("/api/productos/CPU-001", rutaRecibida.get());
    }

    @Test
    void validarExiste_msProductosResponde404_lanzaReglaNegocio() {
        arrancarRespondiendo(404);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
            () -> cliente.validarExiste("FANTASMA"));

        assertEquals("El producto 'FANTASMA' no existe.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 500, 503})
    void validarExiste_otrosErroresHttp_lanzaServicioNoDisponible(int status) {
        arrancarRespondiendo(status);

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
            () -> cliente.validarExiste("CPU-001"));

        assertTrue(ex.getMessage().contains("ms-productos no está disponible"));
    }

    @Test
    void validarExiste_msProductosCaido_lanzaServicioNoDisponible() {
        arrancarRespondiendo(200);
        detener();   // el puerto queda cerrado: conexión rechazada

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.validarExiste("CPU-001"));
    }

    @Test
    @Timeout(15)
    void validarExiste_msProductosNoResponde_aplicaTimeoutYLanzaServicioNoDisponible() {
        servidor.createContext("/", ex -> {
            try {
                Thread.sleep(5000);   // más que el timeout de lectura (3 s)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ex.close();
        });
        arrancar();

        assertThrows(ServicioNoDisponibleException.class, () -> cliente.validarExiste("CPU-001"));
    }
}
