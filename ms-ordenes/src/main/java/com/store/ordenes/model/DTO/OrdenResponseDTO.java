package com.store.ordenes.model.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenResponseDTO {

    private Long id;
    private String usuarioId;
    private String nombreComprador;
    private String apellidosComprador;
    private String correoComprador;
    private String telefono;
    private String region;
    private String comuna;
    private String direccion;
    private String estado;
    private BigDecimal total;
    private LocalDateTime creadoEn;

}
