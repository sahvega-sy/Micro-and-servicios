package com.store.resenia.Controller;

import com.store.resenia.Model.Resenia;
import com.store.resenia.Service.ReseniaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resenias")
public class ReseniaController {

    @Autowired
    private ReseniaService service;

    public ReseniaController(ReseniaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtener",
        description = "Obtiene las reseñas"
    )
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Reseña mostrada exitosamente",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Resenia.class))),
    @ApiResponse(responseCode = "404", description = "No hay reseñas para mostrar")
})
    public List<Resenia> listarTodas() {
        return service.getAllResenias();
    }

    @PostMapping
    @Operation(summary = "Agregar",
        description = "Agrega una reseña a un producto"
    )
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Reseña agregada exitosamente",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Resenia.class)))
})
    public ResponseEntity<Resenia> agregarResenia(@RequestBody Resenia resenia) {
        Resenia guardada = service.saveResenia(resenia);
        return ResponseEntity.status(201).body(guardada);
    }

    @GetMapping("/producto/{productoId}")
    @Operation(summary = "Obtener",
        description = "Obtiene las reseñas por el ID de Producto"
    )
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Reseñas encontradas exitosamente",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Resenia.class))),
    @ApiResponse(responseCode = "404", description = "Reseñas no encontradas")
})
    public List<Resenia> obtenerReseniasProducto(@PathVariable String productoId) {
        return service.getReseniasByProducto(productoId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar",
        description = "Elimina las reseñas por id"
    )
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Reseña eliminada exitosamente",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Resenia.class))),
    @ApiResponse(responseCode = "404", description = "Reseña no encontrada")
})
    public void eliminarResenia(@PathVariable Long id) {
        service.deleteResenia(id);
    }

}
