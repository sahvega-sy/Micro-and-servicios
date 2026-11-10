package com.store.perfil.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

// "orden-service" debe coincidir con el spring.application.name del microservicio de Órdenes en Eureka
@FeignClient(name = "orden-service", path = "/api/ordenes")
public interface OrdenClient {

    @GetMapping("/perfil/{perfilId}")
    List<Object> obtenerOrdenesPorPerfil(@PathVariable("perfilId") String perfilId);

}
