package com.tallerwebi.dominio.indicadores;

import java.time.YearMonth;

/** Variación mensual del IPC (INDEC). estimada = el mes todavía no fue publicado. */
public final class InflacionMensual {

  private final YearMonth mes;
  private final Double porcentaje;
  private final boolean estimada;

  public InflacionMensual(YearMonth mes, Double porcentaje, boolean estimada) {
    this.mes = mes;
    this.porcentaje = porcentaje;
    this.estimada = estimada;
  }

  public YearMonth getMes() {
    return mes;
  }

  public Double getPorcentaje() {
    return porcentaje;
  }

  public boolean isEstimada() {
    return estimada;
  }
}
