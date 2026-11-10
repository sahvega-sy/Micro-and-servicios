package cl.clstore.productos.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import cl.clstore.productos.exception.ReglaNegocioException;
import cl.clstore.productos.exception.ServicioNoDisponibleException;

@Component
public class CategoriaClient {

    private final RestClient rest;

    public CategoriaClient(@Value("${app.services.categorias-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** @throws ReglaNegocioException si la categoría no existe; ServicioNoDisponibleException si ms-categoria no responde. */
    public void validarExiste(String valor) {
        try {
            rest.get().uri("/api/categorias/{valor}", valor).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ReglaNegocioException("La categoría '" + valor + "' no existe. Créala primero en ms-categoria.");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo validar la categoría: ms-categoria no está disponible.");
        }
    }
}
