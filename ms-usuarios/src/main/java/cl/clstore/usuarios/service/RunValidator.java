package cl.clstore.usuarios.service;

import java.util.Locale;

/** Misma regla que validarRunChileno() del front (módulo 11). */
public final class RunValidator {

    private RunValidator() {
    }

    /** Quita puntos, guion y espacios y pasa a mayúsculas: "10.000.013-k" -> "10000013K". */
    public static String normalizar(String run) {
        return run == null ? "" : run.replaceAll("[.\\-\\s]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean esValido(String run) {
        String limpio = normalizar(run);
        if (limpio.length() < 7 || limpio.length() > 9) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dvIngresado = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.matches("\\d+")) {
            return false;
        }
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int esperado = 11 - (suma % 11);
        char dvEsperado = esperado == 11 ? '0' : esperado == 10 ? 'K' : (char) ('0' + esperado);
        return dvIngresado == dvEsperado;
    }
}
