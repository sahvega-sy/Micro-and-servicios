package com.store.contacto.model.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactoRequestDTO {
    private String nombre;
    private String email;
    private String mensaje;
    
}
