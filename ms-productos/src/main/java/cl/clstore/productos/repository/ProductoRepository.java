package cl.clstore.productos.repository;

import cl.clstore.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductoRepository extends JpaRepository<Producto, String>, JpaSpecificationExecutor<Producto> {
}
