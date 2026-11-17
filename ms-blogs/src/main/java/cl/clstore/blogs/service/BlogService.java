package cl.clstore.blogs.service;

import cl.clstore.blogs.dto.BlogRequest;
import cl.clstore.blogs.exception.ConflictoException;
import cl.clstore.blogs.exception.RecursoNoEncontradoException;
import cl.clstore.blogs.exception.ReglaNegocioException;
import cl.clstore.blogs.model.Blog;
import cl.clstore.blogs.repository.BlogRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogService {

    private final BlogRepository repositorio;

    public BlogService(BlogRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Más recientes primero (igual que el front actual). */
    @Transactional(readOnly = true)
    public List<Blog> listar() {
        return repositorio.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Blog obtener(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el blog '" + id + "'."));
    }

    public Blog crear(BlogRequest r) {
        if (r.id() == null || r.id().isBlank()) {
            throw new ReglaNegocioException("El identificador del blog es obligatorio.");
        }
        String id = r.id().trim();
        if (repositorio.existsById(id)) {
            throw new ConflictoException("Ya existe un blog con ese identificador.");
        }
        Blog b = new Blog();
        b.setId(id);
        aplicar(b, r);
        return repositorio.save(b);
    }

    public Blog actualizar(String id, BlogRequest r) {
        Blog b = obtener(id);
        aplicar(b, r);
        return repositorio.save(b);
    }

    public void eliminar(String id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException("No existe el blog '" + id + "'.");
        }
        repositorio.deleteById(id);
    }

    private void aplicar(Blog b, BlogRequest r) {
        List<String> parrafos = r.contenido().stream()
            .filter(p -> p != null && !p.isBlank())
            .map(String::trim)
            .toList();
        if (parrafos.isEmpty()) {
            throw new ReglaNegocioException("El contenido del blog no puede estar vacío.");
        }
        b.setTitulo(r.titulo().trim());
        b.setResumen(r.resumen().trim());
        b.setImagen(vacioANulo(r.imagen()));
        b.setContenido(parrafos);
        b.setFuenteNombre(vacioANulo(r.fuenteNombre()));
        b.setFuenteUrl(vacioANulo(r.fuenteUrl()));
    }

    private String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
