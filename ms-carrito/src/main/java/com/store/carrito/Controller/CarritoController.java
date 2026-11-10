package com.store.carrito.Controller;

import com.store.carrito.Model.Carrito;
import com.store.carrito.Service.CarritoService;

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
@RequestMapping("/api/v1/carritos")
public class CarritoController {

    @Autowired
    private CarritoService service;

    public CarritoController(CarritoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtener",
        description = "Obtiene los carritos"
    )
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Carrito mostrado exitosamente",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Carrito.class))),
    @ApiResponse(responseCode = "404", description = "No hay carritos para mostrar")
})
    public List<Carrito> listarTodas() {
        return service.getAllCarritos();
    }

    @PostMapping
    @Operation(summary = "Agregar",
        description = "Agrega un producto al carrito"
    )
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Producto agregado exitosamente",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Carrito.class)))
})
    public ResponseEntity<Carrito> agregarAlCarrito(@RequestBody Carrito carrito) {
        Carrito guardado = service.saveCarrito(carrito);
        return ResponseEntity.status(201).body(guardado);
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Obtener",
        description = "Obtiene el carrito por el ID de Usuario"
    )
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Carrito encontrado exitosamente",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Carrito.class))),
    @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
})
    public List<Carrito> obtenerCarritoUsuario(@PathVariable String usuarioId) {
        return service.getCarritoByUsuario(usuarioId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar",
        description = "Elimina los carritos por id"
    )
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Carrito eliminado exitosamente",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Carrito.class))),
    @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
})
    public void eliminarItem(@PathVariable Long id) {
        service.deleteItem(id);
    }

}