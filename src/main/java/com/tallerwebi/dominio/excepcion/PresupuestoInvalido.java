package com.tallerwebi.dominio.excepcion;

public class PresupuestoInvalido extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PresupuestoInvalido(String mensaje) {
    super(mensaje);
  }
}
