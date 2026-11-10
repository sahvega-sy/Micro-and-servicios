package com.store.ordenes.controller;
import com.store.ordenes.model.DTO.OrdenRequestDTO;
import com.store.ordenes.model.DTO.OrdenResponseDTO;
import com.store.ordenes.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@Tag(name = "Ordenes", description = "API REST para la gestión de órdenes de compra")
public class OrdenController {

    private final OrdenService ordenService;

    @Autowired
    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las órdenes")
    public ResponseEntity<List<OrdenResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(ordenService.obtenerTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una orden por ID")
    public ResponseEntity<OrdenResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtenerPorId(id));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Obtener órdenes asociadas a un usuario")
    public ResponseEntity<List<OrdenResponseDTO>> obtenerPorUsuarioId(@PathVariable String usuarioId) {
        return ResponseEntity.ok(ordenService.obtenerPorUsuarioId(usuarioId));
    }

    @PostMapping
    @Operation(summary = "Crear una nueva orden")
    public ResponseEntity<OrdenResponseDTO> crear(@RequestBody OrdenRequestDTO requestDTO) {
        OrdenResponseDTO nuevaOrden = ordenService.crear(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaOrden);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar el estado de una orden")
    public ResponseEntity<OrdenResponseDTO> actualizarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(ordenService.actualizarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una orden por ID")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ordenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
