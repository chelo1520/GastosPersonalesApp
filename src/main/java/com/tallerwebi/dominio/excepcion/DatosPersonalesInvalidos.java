package com.tallerwebi.dominio.excepcion;

public class DatosPersonalesInvalidos extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DatosPersonalesInvalidos(String mensaje) {
    super(mensaje);
  }
}
