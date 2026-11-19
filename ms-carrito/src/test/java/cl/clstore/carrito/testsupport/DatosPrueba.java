package cl.clstore.carrito.testsupport;

import cl.clstore.carrito.dto.ItemCarritoResponse;
import cl.clstore.carrito.dto.ProductoResumen;
import cl.clstore.carrito.model.CarritoItem;
import java.math.BigDecimal;
import org.springframework.test.util.ReflectionTestUtils;

/** Fábrica de objetos de prueba para no repetir código en cada test. */
public final class DatosPrueba {

    public static final String USUARIO = "usuario-1";

    private DatosPrueba() {
    }

    /** La entidad no tiene setter de "id" (lo asigna la base de datos): se fija por reflexión; null = entidad nueva. */
    public static CarritoItem item(Long id, String usuarioId, String productoId, int cantidad) {
        CarritoItem i = new CarritoItem();
        i.setUsuarioId(usuarioId);
        i.setProductoId(productoId);
        i.setCantidad(cantidad);
        ReflectionTestUtils.setField(i, "id", id);
        return i;
    }

    public static ProductoResumen producto(String id, String nombre, String precio, Integer descuento, Integer stock) {
        return new ProductoResumen(id, nombre, new BigDecimal(precio), "/img/Logo.png", descuento, stock);
    }

    public static ProductoResumen producto() {
        return producto("CPU-001", "Procesador Ryzen 5", "129990", 15, 10);
    }

    public static ItemCarritoResponse itemRespuesta() {
        return new ItemCarritoResponse(10L, "CPU-001", 2, producto());
    }
}
