package com.tallerwebi.dominio.consejo;

/** Qué regla generó el consejo y cuánta atención pide. */
public enum TipoConsejo {
  // El veredicto del período es el consejo principal: siempre primero, no se descarta
  VEREDICTO_VERDE(Severidad.INFO, true),
  VEREDICTO_AMARILLO(Severidad.ATENCION, true),
  VEREDICTO_ROJO(Severidad.ALERTA, true),
  PRESUPUESTO_DESACTUALIZADO(Severidad.ATENCION, false),
  CATEGORIA_SUBE_MAS_QUE_LA_INFLACION(Severidad.ALERTA, false);

  private final Severidad severidad;
  private final boolean veredicto;

  TipoConsejo(Severidad severidad, boolean veredicto) {
    this.severidad = severidad;
    this.veredicto = veredicto;
  }

  public Severidad getSeveridad() {
    return severidad;
  }

  public boolean isVeredicto() {
    return veredicto;
  }
}
