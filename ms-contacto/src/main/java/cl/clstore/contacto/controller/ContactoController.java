package cl.clstore.contacto.controller;

import cl.clstore.contacto.dto.ContactoRequest;
import cl.clstore.contacto.dto.ContactoResponse;
import cl.clstore.contacto.service.ContactoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contactos")
public class ContactoController {

    private final ContactoService servicio;

    public ContactoController(ContactoService servicio) {
        this.servicio = servicio;
    }

    /** Público: cualquier visitante puede escribir desde el formulario de contacto. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactoResponse crear(@Valid @RequestBody ContactoRequest request) {
        return ContactoResponse.desde(servicio.crear(request));
    }

    /** Personal (Administrador / Vendedor): bandeja de mensajes, más recientes primero. */
    @GetMapping
    public List<ContactoResponse> listar() {
        return servicio.listar().stream().map(ContactoResponse::desde).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        servicio.eliminar(id);
    }
}
