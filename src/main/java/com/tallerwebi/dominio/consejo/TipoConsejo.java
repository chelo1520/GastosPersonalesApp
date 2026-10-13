package com.tallerwebi.dominio.consejo;

/** Qué regla generó el consejo, cuánta atención pide y a qué familia pertenece. */
public enum TipoConsejo {
  // El veredicto del período es la lectura principal: siempre primero
  VEREDICTO_VERDE(Severidad.INFO, Familia.LECTURA, true),
  VEREDICTO_AMARILLO(Severidad.ATENCION, Familia.LECTURA, true),
  VEREDICTO_ROJO(Severidad.ALERTA, Familia.LECTURA, true),
  // Lecturas: conclusiones sobre el período, sin acciones (HU-16)
  BALANCE_DEL_MES_MEJOR(Severidad.INFO, Familia.LECTURA, false),
  BALANCE_DEL_MES_PEOR(Severidad.ATENCION, Familia.LECTURA, false),
  MAYOR_BAJA_REAL(Severidad.INFO, Familia.LECTURA, false),
  MAYOR_SUBA_REAL(Severidad.ATENCION, Familia.LECTURA, false),
  HABITOS_CON_LA_INFLACION(Severidad.INFO, Familia.LECTURA, false),
  HORMIGA_ALTA(Severidad.ATENCION, Familia.LECTURA, false),
  HORMIGA_CONTROLADA(Severidad.INFO, Familia.LECTURA, false),
  CATEGORIA_SUBE_MAS_QUE_LA_INFLACION(Severidad.ALERTA, Familia.LECTURA, false),
  // Propuestas: traen una acción concreta que el usuario decide aplicar
  PRESUPUESTO_DESACTUALIZADO(Severidad.ATENCION, Familia.PROPUESTA, false);

  private final Severidad severidad;
  private final Familia familia;
  private final boolean veredicto;

  TipoConsejo(Severidad severidad, Familia familia, boolean veredicto) {
    this.severidad = severidad;
    this.familia = familia;
    this.veredicto = veredicto;
  }

  public Severidad getSeveridad() {
    return severidad;
  }

  public Familia getFamilia() {
    return familia;
  }

  public boolean isVeredicto() {
    return veredicto;
  }
}
