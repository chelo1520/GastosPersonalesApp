package com.tallerwebi.dominio.consejo;

/** Qué regla generó el consejo y cuánta atención pide. */
public enum TipoConsejo {
  PRESUPUESTO_DESACTUALIZADO(Severidad.ATENCION),
  CATEGORIA_SUBE_MAS_QUE_LA_INFLACION(Severidad.ALERTA),
  GASTO_REAL_EN_ALZA(Severidad.ATENCION);

  private final Severidad severidad;

  TipoConsejo(Severidad severidad) {
    this.severidad = severidad;
  }

  public Severidad getSeveridad() {
    return severidad;
  }
}
