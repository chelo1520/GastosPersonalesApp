package com.tallerwebi.dominio.indicadores;

import java.time.YearMonth;
import java.util.Collection;

/** Variación mensual del IPC (INDEC). estimada = el mes todavía no fue publicado. */
public final class InflacionMensual {

  // La inflación de varios meses seguidos, en porcentaje: se compone mes a mes, no se suma
  public static double acumulada(Collection<InflacionMensual> meses) {
    double factor = 1.0;
    for (InflacionMensual mes : meses) {
      factor *= 1 + mes.getPorcentaje() / 100;
    }
    return (factor - 1) * 100;
  }

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
