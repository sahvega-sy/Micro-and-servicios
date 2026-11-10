package cl.clstore.categoria.controller;

import cl.clstore.categoria.dto.CategoriaRequest;
import cl.clstore.categoria.dto.CategoriaResponse;
import cl.clstore.categoria.service.CategoriaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return servicio.listar().stream().map(CategoriaResponse::desde).toList();
    }

    @GetMapping("/{valor}")
    public CategoriaResponse obtener(@PathVariable("valor") String valor) {
        return CategoriaResponse.desde(servicio.obtener(valor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest request) {
        return CategoriaResponse.desde(servicio.crear(request));
    }

    @PutMapping("/{valor}")
    public CategoriaResponse actualizar(@PathVariable("valor") String valor, @Valid @RequestBody CategoriaRequest request) {
        return CategoriaResponse.desde(servicio.actualizar(valor, request));
    }

    @DeleteMapping("/{valor}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("valor") String valor) {
        servicio.eliminar(valor);
    }
}
