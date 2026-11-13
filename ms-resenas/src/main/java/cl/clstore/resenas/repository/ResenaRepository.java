package cl.clstore.resenas.repository;

import cl.clstore.resenas.model.Resena;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByProductoIdOrderByFechaDescIdDesc(String productoId);
}
