package com.store.productos.model;

import org.hibernate.boot.jaxb.mapping.GenerationTiming;

import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import com.store.productos.DTO.*;
@Data
@Entity
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String descripcion;
    private Double precio;
    private Long stock;
    private Long stockCritico;
    private CategoriaDTO categoria;
    private String destacado;
    private String especificaciones;
    private Double descuento;

}
