package com.store.ordenes.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Orden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", length = 50)
    private String usuarioId;

    @Column(name = "nombre_comprador", nullable = false, length = 100)
    private String nombreComprador;

    @Column(name = "apellidos_comprador", nullable = false, length = 100)
    private String apellidosComprador;

    @Column(name = "correo_comprador", nullable = false, length = 150)
    private String correoComprador;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(nullable = false, length = 100)
    private String comuna;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (this.creadoEn == null) {
            this.creadoEn = LocalDateTime.now();
        }
        if (this.estado == null || this.estado.isBlank()) {
            this.estado = "exitosa";
        }
    }

}
