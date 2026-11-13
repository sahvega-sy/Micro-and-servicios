package cl.clstore.resenas.service;

import cl.clstore.resenas.client.ProductoClient;
import cl.clstore.resenas.dto.ResenaRequest;
import cl.clstore.resenas.exception.RecursoNoEncontradoException;
import cl.clstore.resenas.model.Resena;
import cl.clstore.resenas.repository.ResenaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResenaService {

    private final ResenaRepository repositorio;
    private final ProductoClient productos;

    public ResenaService(ResenaRepository repositorio, ProductoClient productos) {
        this.repositorio = repositorio;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<Resena> listarDeProducto(String productoId) {
        return repositorio.findByProductoIdOrderByFechaDescIdDesc(productoId);
    }

    /** "nombreUsuario" sale del token del usuario autenticado; así no se puede reseñar a nombre de otra persona. */
    public Resena crear(ResenaRequest r, String nombreUsuario) {
        String productoId = r.productoId().trim();
        productos.validarExiste(productoId);
        Resena resena = new Resena();
        resena.setProductoId(productoId);
        resena.setUsuario(nombreUsuario);
        resena.setCalificacion(r.calificacion());
        resena.setComentario(r.comentario().trim());
        return repositorio.save(resena);
    }

    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException("No existe la reseña #" + id + ".");
        }
        repositorio.deleteById(id);
    }
}
