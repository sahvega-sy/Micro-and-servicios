package cl.clstore.contacto.testsupport;

import cl.clstore.contacto.dto.ContactoRequest;
import cl.clstore.contacto.model.Contacto;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

/** Fábrica de objetos de prueba para no repetir código en cada test. */
public final class DatosPrueba {

    private DatosPrueba() {
    }

    /**
     * La entidad no expone setters para "id" ni "creadoEn" (los asigna la base de datos / @PrePersist),
     * así que se fijan por reflexión. Con id = null y creadoEn = null se comporta como una entidad nueva.
     */
    public static Contacto contacto(Long id, String nombre, String email, String mensaje, Instant creadoEn) {
        Contacto c = new Contacto();
        c.setNombre(nombre);
        c.setEmail(email);
        c.setMensaje(mensaje);
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "creadoEn", creadoEn);
        return c;
    }

    public static Contacto contacto() {
        return contacto(1L, "Ana Pérez", "ana@gmail.com", "Hola, tengo una consulta.",
            Instant.parse("2026-01-15T12:00:00Z"));
    }

    public static ContactoRequest request() {
        return new ContactoRequest("Ana Pérez", "ana@gmail.com", "Hola, tengo una consulta.");
    }
}
