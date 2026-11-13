package cl.clstore.usuarios.service;

import cl.clstore.usuarios.model.Usuario;
import cl.clstore.usuarios.repository.UsuarioRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea un Administrador inicial si la tabla está vacía, para poder entrar al panel por primera vez. */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final String correo;
    private final String password;
    private final String run;

    public DataInitializer(UsuarioRepository repositorio, PasswordEncoder encoder,
                           @Value("${app.admin.correo}") String correo,
                           @Value("${app.admin.password}") String password,
                           @Value("${app.admin.run}") String run) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.correo = correo;
        this.password = password;
        this.run = run;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.count() > 0) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID().toString());
        admin.setRun(RunValidator.normalizar(run));
        admin.setNombre("Administrador");
        admin.setApellidos("CLstore");
        admin.setCorreo(correo.toLowerCase());
        admin.setPasswordHash(encoder.encode(password));
        admin.setTipo("Administrador");
        admin.setRegion("Región Metropolitana de Santiago");
        admin.setComuna("Santiago");
        admin.setDireccion("Sin dirección");
        repositorio.save(admin);
        log.warn("Se creó el administrador inicial {} (cambia su contraseña con PUT /api/usuarios/<id> o define ADMIN_PASSWORD).", correo);
    }
}
