package cl.clstore.resenas.testsupport;

import cl.clstore.resenas.dto.ResenaRequest;
import cl.clstore.resenas.model.Resena;
import java.time.LocalDate;
import org.springframework.test.util.ReflectionTestUtils;

/** Fábrica de objetos de prueba para no repetir código en cada test. */
public final class DatosPrueba {

    private DatosPrueba() {
    }

    /**
     * La entidad no expone setters para "id" ni "fecha" (los asigna la base de datos / @PrePersist),
     * así que se fijan por reflexión. Con id = null y fecha = null se comporta como una entidad nueva.
     */
    public static Resena resena(Long id, String productoId, String usuario, int calificacion, String comentario,
                                LocalDate fecha) {
        Resena r = new Resena();
        r.setProductoId(productoId);
        r.setUsuario(usuario);
        r.setCalificacion(calificacion);
        r.setComentario(comentario);
        ReflectionTestUtils.setField(r, "id", id);
        ReflectionTestUtils.setField(r, "fecha", fecha);
        return r;
    }

    public static Resena resena() {
        return resena(1L, "CPU-001", "Ana Pérez", 5, "Excelente procesador.", LocalDate.of(2026, 3, 10));
    }

    public static ResenaRequest request(String productoId, int calificacion, String comentario) {
        return new ResenaRequest(productoId, calificacion, comentario);
    }
}
