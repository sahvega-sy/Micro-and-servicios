package cl.clstore.contacto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import cl.clstore.contacto.model.Contacto;
import java.time.Instant;

public record ContactoResponse(
    Long id,
    String nombre,
    String email,
    String mensaje,
    @JsonProperty("creado_en") Instant creadoEn
) {
    public static ContactoResponse desde(Contacto c) {
        return new ContactoResponse(c.getId(), c.getNombre(), c.getEmail(), c.getMensaje(), c.getCreadoEn());
    }
}
