package com.store.contacto.model.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactoResponseDTO {
    private Long id;
    private String nombre;
    private String email;
    private String mensaje;
    private LocalDateTime creadoEn;

}
