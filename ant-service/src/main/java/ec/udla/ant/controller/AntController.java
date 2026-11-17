package ec.udla.ant.controller;

import ec.udla.ant.model.PuntosLicencia;
import ec.udla.ant.service.PuntosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ant")
public class AntController {
    private final PuntosService service;

    public AntController(PuntosService service) {
        this.service = service;
    }

    @GetMapping("/puntos/{cedula}")
    public PuntosLicencia puntos(@PathVariable String cedula,
                                 @RequestParam(required = false, defaultValue = "") String placa) {
        return service.obtenerPuntos(cedula, placa);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensaje", String.valueOf(e.getReason())));
    }
}
