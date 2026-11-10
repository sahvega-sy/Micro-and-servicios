package com.store.perfil.controller;
import com.store.perfil.model.DTO.PerfilRequestDTO;
import com.store.perfil.model.DTO.PerfilResponseDTO;
import com.store.perfil.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfiles")
@Tag(name = "Perfiles", description = "API REST para la gestión de perfiles de usuario")
public class PerfilController {

    private final PerfilService perfilService;

    @Autowired
    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los perfiles")
    public ResponseEntity<List<PerfilResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(perfilService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un perfil por su ID")
    public ResponseEntity<PerfilResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(perfilService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo perfil")
    public ResponseEntity<PerfilResponseDTO> crear(@RequestBody PerfilRequestDTO requestDTO) {
        PerfilResponseDTO nuevoPerfil = perfilService.crear(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPerfil);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un perfil existente")
    public ResponseEntity<PerfilResponseDTO> actualizar(@PathVariable Long id, @RequestBody PerfilRequestDTO requestDTO) {
        return ResponseEntity.ok(perfilService.actualizar(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un perfil por ID")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        perfilService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
