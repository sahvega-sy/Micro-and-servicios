package com.store.carrito.Model.DTO;

import lombok.Data;

@Data
// DTO (Data Transfer Object) es un objeto simple que se usa solo
// para transportar datos, no para persistirlos, no para lógica,
// y no para relaciones JPA (Define qué datos quiero recibir desde una class).
public class ProductoDTO {
    private Integer id;
    private String nombre;
}
