package com.store.resenia.Repository;

import com.store.resenia.Model.Resenia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReseniaRepository extends JpaRepository<Resenia, Long>{

    List<Resenia> findByProductoId(String productoId);

}
