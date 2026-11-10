package com.store.perfil.model.DTO;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilResponseDTO {
    
    private Long id;
    private String run;
    private String nombre;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;
    private String tipo;
    private String region;
    private String comuna;
    private String direccion;
    private LocalDateTime createdAt;

}
