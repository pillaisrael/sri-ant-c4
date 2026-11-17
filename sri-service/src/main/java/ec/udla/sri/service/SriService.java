package ec.udla.sri.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class SriService {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${sri.url.existe}")        private String urlExiste;
    @Value("${sri.url.contribuyente}") private String urlContribuyente;
    @Value("${sri.url.vehiculo}")      private String urlVehiculo;

    public JsonNode obtenerPersonaNatural(String ruc) {
        if (!ruc.matches("\\d{13}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RUC debe tener 13 digitos");
        }

        String existe = get(urlExiste + enc(ruc)).trim();
        if (!"true".equalsIgnoreCase(existe)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El RUC no es contribuyente del SRI");
        }

        JsonNode json = parse(get(urlContribuyente + enc(ruc)));
        JsonNode persona = json.isArray() ? json.path(0) : json;
        if (persona.isMissingNode() || persona.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontro informacion del contribuyente");
        }

        String tipo = persona.path("tipoContribuyente").asText("");
        if (!tipo.toUpperCase().contains("NATURAL")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El RUC no pertenece a una persona natural (tipo: " + tipo + ")");
        }
        return persona;
    }

    public JsonNode obtenerVehiculo(String placa) {
        String limpia = placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (limpia.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Placa invalida");
        }
        JsonNode json = parse(get(urlVehiculo + enc(limpia)));
        if (json.isNull() || json.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontro el vehiculo");
        }
        return json;
    }

    private String get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SRI respondio " + res.statusCode());
            }
            return res.body();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo conectar con el SRI");
        }
    }

    private JsonNode parse(String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta invalida del SRI");
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
