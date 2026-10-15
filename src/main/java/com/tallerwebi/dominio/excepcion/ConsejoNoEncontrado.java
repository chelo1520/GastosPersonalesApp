package com.tallerwebi.dominio.excepcion;

public class ConsejoNoEncontrado extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConsejoNoEncontrado() {
    super("El consejo no existe");
  }
}
