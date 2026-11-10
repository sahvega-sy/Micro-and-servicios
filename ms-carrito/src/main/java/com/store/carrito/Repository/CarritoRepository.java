package com.store.carrito.Repository;

import com.store.carrito.Model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long>{

    List<Carrito> findByUsuarioId(String usuarioId);

    void deleteByUsuarioId(String usuarioId);

}
