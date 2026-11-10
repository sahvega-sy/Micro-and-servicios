package com.store.perfil.model.DTO;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilRequestDTO {

    private String run;
    private String nombre;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;
    private String tipo;
    private String region;
    private String comuna;
    private String direccion;

}
