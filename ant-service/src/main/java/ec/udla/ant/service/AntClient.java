package ec.udla.ant.service;

import ec.udla.ant.model.PuntosLicencia;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AntClient {
    @Value("${ant.url}")              private String urlPlantilla;
    @Value("${ant.timeout-segundos}") private int timeout;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Pattern PUNTOS = Pattern.compile("(?i)puntos[^0-9]{0,40}(\\d{1,2}(?:[.,]\\d+)?)");

    public PuntosLicencia consultar(String cedula, String placa) throws Exception {
        String url = urlPlantilla.replace("{cedula}", cedula).replace("{placa}", placa == null ? "" : placa);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(timeout))
                .header("User-Agent", "Mozilla/5.0")
                .GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200 || res.body() == null || res.body().isBlank()) {
            throw new IllegalStateException("ANT respondio " + res.statusCode());
        }

        Document doc = Jsoup.parse(res.body());

        String puntos = "No disponible";
        Matcher m = PUNTOS.matcher(doc.text());
        if (m.find()) puntos = m.group(1);

        List<List<String>> citaciones = new ArrayList<>();
        for (Element tr : doc.select("tr")) {
            List<String> celdas = tr.select("td").eachText();
            if (!celdas.isEmpty() && celdas.stream().anyMatch(c -> !c.isBlank())) {
                citaciones.add(celdas);
            }
        }

        return new PuntosLicencia(cedula, placa, puntos, citaciones,
                LocalDateTime.now().toString(), "ANT");
    }
}
