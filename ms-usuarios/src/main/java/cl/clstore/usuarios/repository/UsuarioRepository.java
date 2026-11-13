package cl.clstore.usuarios.repository;

import cl.clstore.usuarios.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByRun(String run);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, String id);
}
