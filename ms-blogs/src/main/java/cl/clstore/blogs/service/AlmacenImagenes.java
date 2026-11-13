package cl.clstore.blogs.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import cl.clstore.blogs.exception.ReglaNegocioException;


@Component
public class AlmacenImagenes {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final String SUBCARPETA = "blogs";
    private static final Map<String, String> EXTENSIONES = Map.of(
        "image/jpeg", "jpg",
        "image/png", "png",
        "image/webp", "webp",
        "image/gif", "gif",
        "image/avif", "avif");

    private final Path carpeta;

    public AlmacenImagenes(@Value("${app.uploads.dir}") String raiz) {
        this.carpeta = Paths.get(raiz).toAbsolutePath().normalize().resolve(SUBCARPETA);
    }

    /** @return URL pública absoluta de la imagen guardada. */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debes adjuntar una imagen.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ReglaNegocioException("La imagen debe pesar 5 MB o menos.");
        }
        String tipo = archivo.getContentType();
        String extension = tipo == null ? null : EXTENSIONES.get(tipo.toLowerCase());
        if (extension == null) {
            throw new ReglaNegocioException("Formato no permitido. Usa JPG, PNG, WEBP, GIF o AVIF.");
        }

        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(carpeta);
            archivo.transferTo(carpeta.resolve(nombre));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen en el servidor.", e);
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/" + SUBCARPETA + "/" + nombre)
            .toUriString();
    }
}
