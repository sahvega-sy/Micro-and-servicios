package cl.clstore.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Sin Spring: validación de RUN chileno (módulo 11), misma regla que el front. */
class RunValidatorTest {

    // ---------------------------------------------------------------- normalizar

    @ParameterizedTest
    @ValueSource(strings = {"10.000.013-k", "10000013K", " 10000013-K ", "10.000.013 k", "10000013-k"})
    void normalizar_quitaPuntosGuionYEspaciosYPasaAMayusculas(String entrada) {
        assertEquals("10000013K", RunValidator.normalizar(entrada));
    }

    @Test
    void normalizar_null_devuelveCadenaVacia() {
        assertEquals("", RunValidator.normalizar(null));
    }

    // ------------------------------------------------------------------ esValido

    @ParameterizedTest
    @ValueSource(strings = {
        "190110222",       // dígito verificador numérico
        "10000013K",       // dígito verificador K
        "100000040",       // dígito verificador 0
        "123456785",
        "111111111",
        "76543216",        // 7 dígitos de cuerpo
        "10.000.013-K",    // con formato
        "10.000.013-k",    // dígito verificador en minúscula
        "19.011.022-2"})
    void esValido_runCorrecto_devuelveTrue(String run) {
        assertTrue(RunValidator.esValido(run), run);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "190110223",       // dígito verificador equivocado
        "123456784",
        "10000013-1",      // debería ser K
        "100000041"})      // debería ser 0
    void esValido_digitoVerificadorIncorrecto_devuelveFalse(String run) {
        assertFalse(RunValidator.esValido(run), run);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "1234567890", "12", "1-9"})
    void esValido_largoFueraDeRango_devuelveFalse(String run) {
        assertFalse(RunValidator.esValido(run), run);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDEFGHI", "1234A6785", "12345678X", "1234 5678 5 x y", "12.34.56-7K9"})
    void esValido_cuerpoConLetrasOCaracteresRaros_devuelveFalse(String run) {
        assertFalse(RunValidator.esValido(run), run);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "-", "..."})
    void esValido_nuloOVacio_devuelveFalse(String run) {
        assertFalse(RunValidator.esValido(run));
    }
}
