package cl.clstore.categoria.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.clstore.categoria.client.ProductoClient;
import cl.clstore.categoria.dto.CategoriaRequest;
import cl.clstore.categoria.exception.ConflictoException;
import cl.clstore.categoria.exception.RecursoNoEncontradoException;
import cl.clstore.categoria.exception.ReglaNegocioException;
import cl.clstore.categoria.model.Categoria;
import cl.clstore.categoria.repository.CategoriaRepository;

@Service
public class CategoriaService {

    private static final String ICONO_POR_DEFECTO = "fa-folder";

    private final CategoriaRepository repositorio;
    private final ProductoClient productos;

    public CategoriaService(CategoriaRepository repositorio, ProductoClient productos) {
        this.repositorio = repositorio;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return repositorio.findAll(Sort.by(Sort.Direction.ASC, "etiqueta"));
    }

    @Transactional(readOnly = true)
    public Categoria obtener(String valor) {
        return repositorio.findById(valor)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría '" + valor + "'."));
    }

    public Categoria crear(CategoriaRequest r) {
        String etiqueta = r.etiqueta().trim();
        String valor = generarValor(etiqueta);
        if (valor.isEmpty()) {
            throw new ReglaNegocioException("Ingresa un nombre válido para la categoría.");
        }
        if (repositorio.existsById(valor)) {
            throw new ConflictoException("Esa categoría ya existe.");
        }
        return repositorio.save(new Categoria(valor, etiqueta, iconoOPorDefecto(r.icono())));
    }

    /** El "valor" no cambia (los productos lo referencian); solo se edita etiqueta e icono. */
    public Categoria actualizar(String valor, CategoriaRequest r) {
        Categoria c = obtener(valor);
        c.setEtiqueta(r.etiqueta().trim());
        if (r.icono() != null && !r.icono().isBlank()) {
            c.setIcono(r.icono().trim());
        }
        return repositorio.save(c);
    }

    public void eliminar(String valor) {
        obtener(valor);
        if (productos.existenProductosEnCategoria(valor)) {
            throw new ConflictoException("No se puede eliminar la categoría porque tiene productos asociados. "
                + "Cambia primero la categoría de esos productos.");
        }
        repositorio.deleteById(valor);
    }

    /** "Consolas & Laptops" -> "consolas_laptops" (misma regla que usaba el front). */
    static String generarValor(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    private String iconoOPorDefecto(String icono) {
        return icono == null || icono.isBlank() ? ICONO_POR_DEFECTO : icono.trim();
    }
}
