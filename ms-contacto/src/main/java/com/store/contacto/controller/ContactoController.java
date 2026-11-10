package com.store.contacto.controller;
import com.store.contacto.model.DTO.ContactoRequestDTO;
import com.store.contacto.model.DTO.ContactoResponseDTO;
import com.store.contacto.service.ContactoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contactos")
@Tag(name = "Contactos", description = "API REST para la recepción y gestión de mensajes de contacto")
public class ContactoController {

    private final ContactoService contactoService;

    @Autowired
    public ContactoController(ContactoService contactoService) {
        this.contactoService = contactoService;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los mensajes de contacto")
    public ResponseEntity<List<ContactoResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(contactoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un mensaje de contacto por ID")
    public ResponseEntity<ContactoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contactoService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Enviar un nuevo mensaje de contacto")
    public ResponseEntity<ContactoResponseDTO> crear(@RequestBody ContactoRequestDTO requestDTO) {
        ContactoResponseDTO nuevoContacto = contactoService.crear(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoContacto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un mensaje de contacto por ID")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        contactoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
