package com.store.perfil.repository;
import com.store.perfil.model.perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<perfil, Long> {
    Optional<perfil> findByRun(String run);
    Optional<perfil> findByCorreo(String correo);
}
