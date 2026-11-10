package com.store.ordenes.repository;
import com.store.ordenes.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {
    List<Orden> findByUsuarioId(String usuarioId);
    List<Orden> findByCorreoComprador(String correoComprador);
    List<Orden> findByEstado(String estado);
}
