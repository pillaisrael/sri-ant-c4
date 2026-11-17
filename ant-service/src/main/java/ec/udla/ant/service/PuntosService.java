package ec.udla.ant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.udla.ant.model.PuntosLicencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class PuntosService {
    private static final Logger log = LoggerFactory.getLogger(PuntosService.class);

    private final AntClient ant;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${cache.ttl-fresco-minutos}") private long ttlFresco;
    @Value("${cache.ttl-respaldo-dias}")  private long ttlRespaldo;

    public PuntosService(AntClient ant, StringRedisTemplate redis) {
        this.ant = ant;
        this.redis = redis;
    }

    public PuntosLicencia obtenerPuntos(String cedula, String placa) {
        if (!cedula.matches("\\d{10}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cedula debe tener 10 digitos");
        }
        placa = placa == null ? "" : placa.toUpperCase().replaceAll("[^A-Z0-9]", "");

        String claveFresca = "ant:fresco:" + cedula + ":" + placa;
        String claveRespaldo = "ant:respaldo:" + cedula + ":" + placa;

        PuntosLicencia enCache = leer(claveFresca);
        if (enCache != null) {
            log.info("CACHE HIT {}", claveFresca);
            return enCache.conFuente("CACHE");
        }

        try {
            PuntosLicencia nuevo = ant.consultar(cedula, placa);
            guardar(claveFresca, nuevo, Duration.ofMinutes(ttlFresco));
            guardar(claveRespaldo, nuevo, Duration.ofDays(ttlRespaldo));
            log.info("ANT OK, guardado en cache {}", claveFresca);
            return nuevo;
        } catch (Exception e) {
            log.warn("ANT no disponible ({}). Buscando respaldo en cache.", e.getMessage());
            PuntosLicencia respaldo = leer(claveRespaldo);
            if (respaldo != null) {
                return respaldo.conFuente("CACHE_RESPALDO");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La ANT no esta disponible y no hay datos en cache. Intente mas tarde.");
        }
    }

    private PuntosLicencia leer(String clave) {
        try {
            String json = redis.opsForValue().get(clave);
            return json == null ? null : mapper.readValue(json, PuntosLicencia.class);
        } catch (Exception e) {
            log.warn("No se pudo leer el cache: {}", e.getMessage());
            return null;
        }
    }

    private void guardar(String clave, PuntosLicencia valor, Duration ttl) {
        try {
            redis.opsForValue().set(clave, mapper.writeValueAsString(valor), ttl);
        } catch (Exception e) {
            log.warn("No se pudo guardar en cache: {}", e.getMessage());
        }
    }
}
