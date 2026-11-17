package cl.clstore.productos.controller;

import cl.clstore.productos.dto.StockRequest;
import cl.clstore.productos.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Uso interno: solo acepta un JWT de servicio (rol "Servicio") emitido por ms-ordenes. */
@RestController
@RequestMapping("/api/productos/stock")
public class StockController {

    private final StockService servicio;

    public StockController(StockService servicio) {
        this.servicio = servicio;
    }

    /** 204 si todo se descontó; 409 (con el detalle) si algún producto no tiene stock suficiente. */
    @PostMapping("/descontar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void descontar(@Valid @RequestBody StockRequest request) {
        servicio.descontar(request.items());
    }

    @PostMapping("/reponer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reponer(@Valid @RequestBody StockRequest request) {
        servicio.reponer(request.items());
    }
}
