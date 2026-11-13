package cl.clstore.productos.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.clstore.productos.client.CategoriaClient;
import cl.clstore.productos.dto.ProductoRequest;
import cl.clstore.productos.exception.ConflictoException;
import cl.clstore.productos.exception.RecursoNoEncontradoException;
import cl.clstore.productos.exception.ReglaNegocioException;
import cl.clstore.productos.model.Producto;
import cl.clstore.productos.repository.ProductoRepository;
import cl.clstore.productos.repository.ProductoSpecs;

@Service
public class ProductoService {

    private static final int LIMITE_MAXIMO = 200;
    private static final String IMAGEN_POR_DEFECTO = "/img/Logo.png";

    private final ProductoRepository repositorio;
    private final CategoriaClient categorias;

    public ProductoService(ProductoRepository repositorio, CategoriaClient categorias) {
        this.repositorio = repositorio;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar(String categoria, String texto, boolean soloOfertas,
                                 String excluirId, List<String> ids, String orden, Integer limite) {
        Specification<Producto> filtro = ProductoSpecs.filtrar(categoria, texto, soloOfertas, excluirId, ids);
        Sort sort = ordenar(orden);
        if (limite != null && limite > 0) {
            return repositorio.findAll(filtro, PageRequest.of(0, Math.min(limite, LIMITE_MAXIMO), sort)).getContent();
        }
        return repositorio.findAll(filtro, sort);
    }

    @Transactional(readOnly = true)
    public Producto obtener(String id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto '" + id + "'."));
    }

    public Producto crear(ProductoRequest r) {
        if (r.id() == null || r.id().isBlank()) {
            throw new ReglaNegocioException("El código del producto es obligatorio (mínimo 3 caracteres).");
        }
        String id = r.id().trim();
        if (repositorio.existsById(id)) {
            throw new ConflictoException("Ya existe un producto con ese código.");
        }
        categorias.validarExiste(r.categoria().trim());

        Producto p = new Producto();
        p.setId(id);
        aplicar(p, r);
        return repositorio.save(p);
    }

    public Producto actualizar(String id, ProductoRequest r) {
        Producto p = obtener(id);
        categorias.validarExiste(r.categoria().trim());
        aplicar(p, r);
        return repositorio.save(p);
    }

    public void eliminar(String id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException("No existe el producto '" + id + "'.");
        }
        repositorio.deleteById(id);
    }

    private void aplicar(Producto p, ProductoRequest r) {
        String nombre = r.nombre().trim();
        p.setNombre(nombre);
        p.setDescripcion(r.descripcion());
        p.setPrecio(r.precio());
        p.setStock(r.stock());
        p.setStockCritico(r.stockCritico());
        p.setCategoria(r.categoria().trim());
        p.setImagen(r.imagen() == null || r.imagen().isBlank() ? IMAGEN_POR_DEFECTO : r.imagen().trim());
        p.setBusqueda(nombre.toLowerCase());
        p.setDescuentoPorcentaje(r.descuentoPorcentaje());
        if (r.imagenes() != null) {
            p.setImagenes(r.imagenes());
        }
        if (r.destacado() != null) {
            p.setDestacado(r.destacado());
        }
        if (r.especificaciones() != null) {
            p.setEspecificaciones(r.especificaciones());
        }
    }

    private Sort ordenar(String orden) {
        if (orden == null) {
            return Sort.by(Sort.Direction.ASC, "nombre");
        }
        return switch (orden) {
            case "creado_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "creado_desc" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "descuento_desc" -> Sort.by(Sort.Direction.DESC, "descuentoPorcentaje");
            default -> Sort.by(Sort.Direction.ASC, "nombre");
        };
    }
}
