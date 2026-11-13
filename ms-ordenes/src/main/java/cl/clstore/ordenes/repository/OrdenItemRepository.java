package cl.clstore.ordenes.repository;

import cl.clstore.ordenes.model.OrdenItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenItemRepository extends JpaRepository<OrdenItem, Long> {
}
