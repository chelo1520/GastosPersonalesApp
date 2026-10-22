package com.tallerwebi.dominio.excepcion;

import com.tallerwebi.dominio.ReglasDeContrasena;

/** La contraseña nueva no cumple las reglas de ReglasDeContrasena. */
public class ContrasenaInvalida extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ContrasenaInvalida() {
    super(ReglasDeContrasena.MENSAJE);
  }
}
