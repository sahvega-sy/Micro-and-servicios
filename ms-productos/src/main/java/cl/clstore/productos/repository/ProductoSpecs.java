package cl.clstore.productos.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import cl.clstore.productos.model.Producto;
import jakarta.persistence.criteria.Predicate;

public final class ProductoSpecs {

    private ProductoSpecs() {
    }

    public static Specification<Producto> filtrar(String categoria, String texto, boolean soloOfertas,
                                                  String excluirId, List<String> ids) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (hayTexto(categoria)) {
                condiciones.add(cb.equal(root.get("categoria"), categoria.trim()));
            }
            if (hayTexto(texto)) {
                String patron = "%" + texto.trim().toLowerCase() + "%";
                condiciones.add(cb.or(
                    cb.like(cb.lower(root.get("nombre")), patron),
                    cb.like(cb.lower(root.get("busqueda")), patron)));
            }
            if (soloOfertas) {
                condiciones.add(cb.isNotNull(root.get("descuentoPorcentaje")));
            }
            if (hayTexto(excluirId)) {
                condiciones.add(cb.notEqual(root.get("id"), excluirId.trim()));
            }
            if (ids != null && !ids.isEmpty()) {
                condiciones.add(root.get("id").in(ids));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    private static boolean hayTexto(String s) {
        return s != null && !s.isBlank();
    }
}
