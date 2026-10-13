package com.tallerwebi.dominio.excepcion;

/**
 * La API de indicadores no respondió y no hay datos cacheados para responder
 */
public class IndicadoresNoDisponibles extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public IndicadoresNoDisponibles(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }

  public IndicadoresNoDisponibles(String mensaje) {
    super(mensaje);
  }
}
