package com.store.carrito.Model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "carrito_items",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_carrito_usuario_producto",
           columnNames = {"usuario_id", "producto_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;

    @Column(name = "producto_id", nullable = false, length = 100)
    private String productoId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
