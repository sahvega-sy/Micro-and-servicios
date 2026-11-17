import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Genera un JWT (HS256) de PRUEBA para llamar a los endpoints de administración.
 *
 *   java scripts/GenerarToken.java                      -> rol Administrador, secreto de desarrollo
 *   java scripts/GenerarToken.java <secreto> Vendedor   -> secreto y rol personalizados
 *
 * El secreto debe ser el mismo que JWT_SECRET de los microservicios.
 */
public class GenerarToken {

    public static void main(String[] args) throws Exception {
        String secreto = args.length > 0 ? args[0] : "clstore-dev-secret-cambiar-en-produccion-32b";
        String rol = args.length > 1 ? args[1] : "Administrador";

        long ahora = System.currentTimeMillis() / 1000;
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"usuario-prueba\",\"rol\":\"" + rol + "\",\"iat\":" + ahora
            + ",\"exp\":" + (ahora + 8 * 3600) + "}";

        String firmando = base64Url(header.getBytes(StandardCharsets.UTF_8)) + "."
            + base64Url(payload.getBytes(StandardCharsets.UTF_8));

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = base64Url(mac.doFinal(firmando.getBytes(StandardCharsets.UTF_8)));

        System.out.println(firmando + "." + firma);
    }

    private static String base64Url(byte[] datos) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }
}
