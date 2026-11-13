package cl.clstore.ordenes.repository;

import cl.clstore.ordenes.model.Orden;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    List<Orden> findAllByOrderByCreadoEnDesc();

    List<Orden> findByEstadoOrderByCreadoEnDesc(String estado);

    List<Orden> findByUsuarioIdOrderByCreadoEnDesc(String usuarioId);
}
