package cl.clstore.blogs.controller;

import cl.clstore.blogs.service.AlmacenImagenes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** POST /api/blogs/imagenes  (multipart, campo "archivo")  ->  201 { "url": "http://.../uploads/..." } */
@Tag(name = "Imágenes de blogs", description = "Subida de imágenes (multipart, máximo 5 MB)")
@RestController
@RequestMapping("/api/blogs/imagenes")
public class ImagenController {

    private final AlmacenImagenes almacen;

    public ImagenController(AlmacenImagenes almacen) {
        this.almacen = almacen;
    }

    @Operation(summary = "Subir una imagen de blog (campo multipart 'archivo')")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> subir(@RequestParam("archivo") MultipartFile archivo) {
        return Map.of("url", almacen.guardar(archivo));
    }
}
