package cl.clstore.productos.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.clstore.productos.service.AlmacenImagenes;

@RestController
@RequestMapping("/api/productos/imagenes")
public class ImagenController {

    private final AlmacenImagenes almacen;

    public ImagenController(AlmacenImagenes almacen) {
        this.almacen = almacen;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> subir(@RequestParam("archivo") MultipartFile archivo) {
        return Map.of("url", almacen.guardar(archivo));
    }
}
