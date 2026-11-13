package cl.clstore.contacto.repository;

import cl.clstore.contacto.model.Contacto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<Contacto, Long> {

    List<Contacto> findAllByOrderByCreadoEnDesc();
}
