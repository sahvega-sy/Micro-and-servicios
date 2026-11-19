package cl.clstore.ordenes.testsupport;

import cl.clstore.ordenes.dto.OrdenRequest;
import cl.clstore.ordenes.dto.OrdenResponse;
import cl.clstore.ordenes.dto.ProductoResumen;
import cl.clstore.ordenes.model.Orden;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

/** Fábrica de objetos de prueba para no repetir código en cada test. */
public final class DatosPrueba {

    public static final String USUARIO = "usuario-1";

    private DatosPrueba() {
    }

    public static OrdenRequest.Item linea(String productoId, int cantidad) {
        return new OrdenRequest.Item(productoId, cantidad);
    }

    /** Request válido; el teléfono es opcional. */
    public static OrdenRequest request(OrdenRequest.Item... items) {
        return new OrdenRequest("Ana", "Pérez", "ana@gmail.com", "+56911111111",
            "Región Metropolitana de Santiago", "Santiago", "Calle Falsa 123", List.of(items));
    }

    public static OrdenRequest request() {
        return request(linea("CPU-001", 2));
    }

    public static ProductoResumen producto(String id, String nombre, String precio, Integer descuento, Integer stock) {
        return new ProductoResumen(id, nombre, new BigDecimal(precio), "/img/Logo.png", descuento, stock);
    }

    /**
     * La entidad no expone setter de "id" ni de "creadoEn" (los asignan la base de datos / @PrePersist),
     * así que se fijan por reflexión. Con id = null y creadoEn = null se comporta como una entidad nueva.
     */
    public static Orden orden(Long id, String usuarioId, String estado, String total, Instant creadoEn) {
        Orden o = new Orden();
        o.setUsuarioId(usuarioId);
        o.setNombreComprador("Ana");
        o.setApellidosComprador("Pérez");
        o.setCorreoComprador("ana@gmail.com");
        o.setTelefono("+56911111111");
        o.setRegion("Región Metropolitana de Santiago");
        o.setComuna("Santiago");
        o.setDireccion("Calle Falsa 123");
        o.setTotal(new BigDecimal(total));
        o.setEstado(estado);
        ReflectionTestUtils.setField(o, "id", id);
        ReflectionTestUtils.setField(o, "creadoEn", creadoEn);
        return o;
    }

    public static Orden orden() {
        return orden(5L, USUARIO, Orden.EXITOSA, "280974", Instant.parse("2026-01-15T12:00:00Z"));
    }

    public static OrdenResponse ordenResponse() {
        return OrdenResponse.desde(orden(), false);
    }
}
