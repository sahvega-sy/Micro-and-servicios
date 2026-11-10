package com.store.ordenes.model.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenRequestDTO {

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

}
