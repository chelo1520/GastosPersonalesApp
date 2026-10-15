package com.tallerwebi.dominio.consejo;

/** Cuánta atención pide el consejo; el estilo es el sufijo de la clase CSS del chip. */
public enum Severidad {
  INFO("ok"),
  ATENCION("warn"),
  ALERTA("over");

  private final String estilo;

  Severidad(String estilo) {
    this.estilo = estilo;
  }

  public String getEstilo() {
    return estilo;
  }
}
