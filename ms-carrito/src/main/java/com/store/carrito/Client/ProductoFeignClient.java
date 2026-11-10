package com.store.carrito.Client;

import com.store.carrito.Model.DTO.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// El name debe ser el spring.application.name del otro microservicio
@FeignClient(name = "ms-producto", url = "localhost:8082")

/* JuegoFeignClient es el "puente" que permite a usuarios
comunicarse con Juegos usando HTTP,
pero como si fuera una llamada a un método Java. */

public interface ProductoFeignClient {
    @GetMapping("/api/v1/productos/{id}")
    ProductoDTO obtenerProductoPorId(@PathVariable("id") Integer id);
}
