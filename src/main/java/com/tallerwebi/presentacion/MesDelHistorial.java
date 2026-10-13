package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.resumen.ResumenDeMes;

/**
 * Un mes del gráfico de historial, con su nombre corto para el eje (ej. "Oct 2026")
 */
public class MesDelHistorial {

  private final String etiqueta;
  private final ResumenDeMes resumen;

  MesDelHistorial(ResumenDeMes resumen) {
    this.etiqueta = NombreDeMes.corto(resumen.getMes());
    this.resumen = resumen;
  }

  public String getEtiqueta() {
    return etiqueta;
  }

  public ResumenDeMes getResumen() {
    return resumen;
  }
}
