package cl.clstore.ordenes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Datos del comprador y los productos a comprar. El precio NO viaja: lo lee ms-ordenes de ms-productos.
 * Compra de invitado: se envía sin token. Con sesión: se envía el JWT y la orden queda asociada al usuario.
 */
public record OrdenRequest(
    @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String nombre,
    @NotBlank(message = "es obligatorio") @Size(max = 100, message = "máximo 100 caracteres") String apellidos,
    @NotBlank(message = "es obligatorio") @Email(message = "no es un correo válido") @Size(max = 150, message = "máximo 150 caracteres") String correo,
    @Size(max = 30, message = "máximo 30 caracteres") String telefono,
    @NotBlank(message = "es obligatoria") @Size(max = 100, message = "máximo 100 caracteres") String region,
    @NotBlank(message = "es obligatoria") @Size(max = 100, message = "máximo 100 caracteres") String comuna,
    @NotBlank(message = "es obligatoria") @Size(max = 255, message = "máximo 255 caracteres") String direccion,
    @NotEmpty(message = "debe incluir al menos un producto") List<@Valid Item> items
) {

    public record Item(
        @JsonProperty("producto_id") @NotBlank(message = "es obligatorio") String productoId,
        @Min(value = 1, message = "debe ser al menos 1") @Max(value = 99, message = "máximo 99") int cantidad
    ) {
    }
}
