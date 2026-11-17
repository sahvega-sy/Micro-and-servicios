package cl.clstore.productos.repository;

import cl.clstore.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, String>, JpaSpecificationExecutor<Producto> {

    /** Resta stock solo si alcanza; devuelve 0 filas si no hay suficiente (operación atómica, sin condición de carrera). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Producto p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad")
    int descontarStock(@Param("id") String id, @Param("cantidad") int cantidad);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Producto p set p.stock = p.stock + :cantidad where p.id = :id")
    int reponerStock(@Param("id") String id, @Param("cantidad") int cantidad);
}
