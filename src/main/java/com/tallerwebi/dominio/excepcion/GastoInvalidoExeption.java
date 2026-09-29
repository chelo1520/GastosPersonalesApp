package com.tallerwebi.dominio.excepcion;

/**
 * GastoInvalidoExeption
 */
public class GastoInvalidoExeption extends RuntimeException {

    /* Identificador para la serialización de la clase, requerido por PMD en excepciones */
    private static final long serialVersionUID = 1L;

    public GastoInvalidoExeption(String message) {
        super(message);
    }

}
