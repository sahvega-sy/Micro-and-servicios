package cl.clstore.blogs.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.blogs.controller.ImagenController;
import cl.clstore.blogs.service.AlmacenImagenes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** WebConfig publica la carpeta de imágenes en GET /uploads/** y SecurityConfig la deja pasar sin token. */
@WebMvcTest(ImagenController.class)
@Import({SecurityConfig.class, WebConfig.class})
class UploadsPublicosTest {

    /** Misma carpeta que app.uploads.dir en src/test/resources/application.properties. */
    private static final Path RAIZ = Paths.get("target/uploads-test");
    private static final byte[] CONTENIDO = {1, 2, 3, 4, 5};

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AlmacenImagenes almacen;

    @BeforeEach
    void crearArchivo() throws IOException {
        Path carpeta = RAIZ.resolve("blogs");
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve("prueba.png"), CONTENIDO);
    }

    @Test
    void imagenSubida_sePuedeLeerSinToken() throws Exception {
        mvc.perform(get("/uploads/blogs/prueba.png"))
            .andExpect(status().isOk())
            .andExpect(content().bytes(CONTENIDO));
    }

    @Test
    void imagenSubida_seEntregaConCacheDeTreintaDias() throws Exception {
        mvc.perform(get("/uploads/blogs/prueba.png"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("max-age=2592000")));
    }

    @Test
    void imagenInexistente_devuelve404() throws Exception {
        mvc.perform(get("/uploads/blogs/no-existe.png")).andExpect(status().isNotFound());
    }
}
