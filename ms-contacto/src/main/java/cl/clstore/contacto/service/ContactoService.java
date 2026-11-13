package cl.clstore.contacto.service;

import cl.clstore.contacto.dto.ContactoRequest;
import cl.clstore.contacto.exception.RecursoNoEncontradoException;
import cl.clstore.contacto.model.Contacto;
import cl.clstore.contacto.repository.ContactoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactoService {

    private final ContactoRepository repositorio;

    public ContactoService(ContactoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Contacto crear(ContactoRequest r) {
        Contacto c = new Contacto();
        c.setNombre(r.nombre().trim());
        c.setEmail(r.email().trim());
        c.setMensaje(r.mensaje().trim());
        return repositorio.save(c);
    }

    @Transactional(readOnly = true)
    public List<Contacto> listar() {
        return repositorio.findAllByOrderByCreadoEnDesc();
    }

    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException("No existe el mensaje #" + id + ".");
        }
        repositorio.deleteById(id);
    }
}
