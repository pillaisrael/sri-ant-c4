package ec.udla.sri.controller;

import com.fasterxml.jackson.databind.JsonNode;
import ec.udla.sri.service.SriService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/sri")
public class SriController {
    private final SriService sri;

    public SriController(SriService sri) {
        this.sri = sri;
    }

    @GetMapping("/contribuyente/{ruc}")
    public JsonNode contribuyente(@PathVariable String ruc) {
        return sri.obtenerPersonaNatural(ruc);
    }

    @GetMapping("/vehiculo/{placa}")
    public JsonNode vehiculo(@PathVariable String placa) {
        return sri.obtenerVehiculo(placa);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensaje", String.valueOf(e.getReason())));
    }
}
