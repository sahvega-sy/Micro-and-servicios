package cl.clstore.categoria.repository;

import cl.clstore.categoria.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {
}
