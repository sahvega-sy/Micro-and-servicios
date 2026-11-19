package cl.clstore.productos.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.clstore.productos.exception.ReglaNegocioException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Prueba sin Spring: una carpeta temporal real y una petición HTTP simulada para armar la URL pública. */
class AlmacenImagenesTest {

    private static final int CINCO_MB = 5 * 1024 * 1024;

    @TempDir
    Path raiz;

    private AlmacenImagenes almacen;

    @BeforeEach
    void preparar() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        almacen = new AlmacenImagenes(raiz.toString());
    }

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private List<Path> archivosGuardados() throws IOException {
        try (Stream<Path> s = Files.list(raiz.resolve("productos"))) {
            return s.toList();
        }
    }

    @Test
    void guardar_pngValido_escribeElArchivoYDevuelveLaUrlPublica() throws IOException {
        byte[] contenido = {1, 2, 3, 4};

        String url = almacen.guardar(new MockMultipartFile("archivo", "foto.png", "image/png", contenido));

        List<Path> guardados = archivosGuardados();
        assertEquals(1, guardados.size());
        Path archivo = guardados.get(0);
        assertArrayEquals(contenido, Files.readAllBytes(archivo));
        assertTrue(archivo.getFileName().toString().matches("[0-9a-f-]{36}\\.png"), "nombre UUID + extensión");
        assertEquals("http://localhost/uploads/productos/" + archivo.getFileName(), url);
    }

    @ParameterizedTest
    @CsvSource({"image/jpeg,jpg", "image/png,png", "image/webp,webp", "image/gif,gif",
        "image/avif,avif", "IMAGE/PNG,png"})
    void guardar_tipoPermitido_usaLaExtensionCorrespondiente(String tipo, String extension) throws IOException {
        almacen.guardar(new MockMultipartFile("archivo", "x", tipo, new byte[] {1}));

        assertTrue(archivosGuardados().get(0).getFileName().toString().endsWith("." + extension));
    }

    @Test
    void guardar_nombreOriginalPeligroso_seDescartaYNoSaleDeLaCarpeta() throws IOException {
        almacen.guardar(new MockMultipartFile("archivo", "../../evil.png", "image/png", new byte[] {1}));

        assertEquals(1, archivosGuardados().size());
        assertFalse(archivosGuardados().get(0).getFileName().toString().contains("evil"));
        try (Stream<Path> s = Files.list(raiz)) {
            assertEquals(List.of("productos"), s.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void guardar_dosArchivosIguales_generanNombresDistintos() throws IOException {
        MockMultipartFile f = new MockMultipartFile("archivo", "a.png", "image/png", new byte[] {1});

        almacen.guardar(f);
        almacen.guardar(f);

        assertEquals(2, archivosGuardados().size());
    }

    @Test
    void guardar_archivoNull_lanzaReglaNegocio() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> almacen.guardar(null));

        assertEquals("Debes adjuntar una imagen.", ex.getMessage());
    }

    @Test
    void guardar_archivoVacio_lanzaReglaNegocio() {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "a.png", "image/png", new byte[0]);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> almacen.guardar(vacio));

        assertEquals("Debes adjuntar una imagen.", ex.getMessage());
    }

    @Test
    void guardar_masDeCincoMb_lanzaReglaNegocio() {
        MockMultipartFile grande = new MockMultipartFile("archivo", "a.png", "image/png", new byte[CINCO_MB + 1]);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> almacen.guardar(grande));

        assertEquals("La imagen debe pesar 5 MB o menos.", ex.getMessage());
    }

    @Test
    void guardar_exactamenteCincoMb_estaPermitido() {
        MockMultipartFile limite = new MockMultipartFile("archivo", "a.png", "image/png", new byte[CINCO_MB]);

        assertDoesNotThrow(() -> almacen.guardar(limite));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"application/pdf", "text/html", "image/svg+xml", "application/octet-stream"})
    void guardar_formatoNoPermitido_lanzaReglaNegocio(String tipo) {
        MockMultipartFile f = new MockMultipartFile("archivo", "a.bin", tipo, new byte[] {1});

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> almacen.guardar(f));

        assertEquals("Formato no permitido. Usa JPG, PNG, WEBP, GIF o AVIF.", ex.getMessage());
    }

    @Test
    void guardar_siNoSePuedeEscribirEnDisco_lanzaUncheckedIOException() throws IOException {
        Files.createFile(raiz.resolve("productos"));   // "productos" es un archivo: no se puede crear la carpeta
        MockMultipartFile f = new MockMultipartFile("archivo", "a.png", "image/png", new byte[] {1});

        assertThrows(UncheckedIOException.class, () -> almacen.guardar(f));
    }
}
