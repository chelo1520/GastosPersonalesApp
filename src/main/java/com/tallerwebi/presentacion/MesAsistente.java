package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.asistente.MesGastoReal;

/** Un mes del asistente con su nombre para mostrar ("Oct 2026"). */
public class MesAsistente {

  private final String etiqueta;
  private final MesGastoReal datos;

  public MesAsistente(MesGastoReal datos) {
    this.datos = datos;
    this.etiqueta = NombreDeMes.corto(datos.getMes()) + (datos.isEnCurso() ? " (en curso)" : "");
  }

  public String getEtiqueta() {
    return etiqueta;
  }

  public MesGastoReal getDatos() {
    return datos;
  }
}
