package cl.clstore.carrito.repository;

import cl.clstore.carrito.model.CarritoItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {

    List<CarritoItem> findByUsuarioIdOrderByIdAsc(String usuarioId);

    Optional<CarritoItem> findByUsuarioIdAndProductoId(String usuarioId, String productoId);

    void deleteByUsuarioIdAndProductoId(String usuarioId, String productoId);

    void deleteByUsuarioId(String usuarioId);
}
