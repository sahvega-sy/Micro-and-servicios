package cl.clstore.productos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.clstore.productos.config.SecurityConfig;
import cl.clstore.productos.exception.ReglaNegocioException;
import cl.clstore.productos.service.AlmacenImagenes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(ImagenController.class)
@Import(SecurityConfig.class)
class ImagenControllerTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_Administrador");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AlmacenImagenes almacen;

    private static MockMultipartFile imagen() {
        return new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1, 2, 3});
    }

    @Test
    void subir_imagenValida_devuelve201ConLaUrl() throws Exception {
        when(almacen.guardar(any(MultipartFile.class)))
            .thenReturn("http://localhost/uploads/productos/abc.png");

        mvc.perform(multipart("/api/productos/imagenes").file(imagen()).with(jwt().authorities(ADMIN)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.url").value("http://localhost/uploads/productos/abc.png"));
    }

    @Test
    void subir_formatoNoPermitido_devuelve400() throws Exception {
        when(almacen.guardar(any(MultipartFile.class)))
            .thenThrow(new ReglaNegocioException("Formato no permitido. Usa JPG, PNG, WEBP, GIF o AVIF."));

        mvc.perform(multipart("/api/productos/imagenes").file(imagen()).with(jwt().authorities(ADMIN)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Formato no permitido. Usa JPG, PNG, WEBP, GIF o AVIF."));
    }

    @Test
    void subir_archivoDemasiadoGrande_devuelve413() throws Exception {
        when(almacen.guardar(any(MultipartFile.class)))
            .thenThrow(new MaxUploadSizeExceededException(5L * 1024 * 1024));

        mvc.perform(multipart("/api/productos/imagenes").file(imagen()).with(jwt().authorities(ADMIN)))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.detail").value("La imagen debe pesar 5 MB o menos."));
    }

    @Test
    void subir_sinElCampoArchivo_devuelve400() throws Exception {
        mvc.perform(multipart("/api/productos/imagenes").with(jwt().authorities(ADMIN)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(almacen);
    }
}
