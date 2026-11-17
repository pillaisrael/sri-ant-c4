package ec.udla.ant.model;

import java.util.List;

public record PuntosLicencia(
        String cedula,
        String placa,
        String puntos,
        List<List<String>> citaciones,
        String fechaConsulta,
        String fuente
) {
    public PuntosLicencia conFuente(String nuevaFuente) {
        return new PuntosLicencia(cedula, placa, puntos, citaciones, fechaConsulta, nuevaFuente);
    }
}
