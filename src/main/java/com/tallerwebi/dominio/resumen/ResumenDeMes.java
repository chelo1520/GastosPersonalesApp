package com.tallerwebi.dominio.resumen;

import java.time.YearMonth;

/**
 * Totales de un mes para el historial: lo gastado, cuánto fue hormiga y el presupuesto de ese mes
 */
public class ResumenDeMes {

  private final YearMonth mes;
  private final Double gastado;
  private final Double hormiga;
  private final Double presupuesto;
  // El mes en curso todavía no cerró: su total va a seguir subiendo
  private final boolean enCurso;

  public ResumenDeMes(
    YearMonth mes,
    Double gastado,
    Double hormiga,
    Double presupuesto,
    boolean enCurso
  ) {
    this.mes = mes;
    this.gastado = gastado;
    this.hormiga = hormiga;
    this.presupuesto = presupuesto;
    this.enCurso = enCurso;
  }

  public YearMonth getMes() {
    return mes;
  }

  public Double getGastado() {
    return gastado;
  }

  public Double getHormiga() {
    return hormiga;
  }

  // Lo gastado que no es hormiga: el segmento de arriba de la barra apilada
  public Double getResto() {
    return gastado - hormiga;
  }

  public Double getPresupuesto() {
    return presupuesto;
  }

  public Double getPorcentajeUsado() {
    return presupuesto > 0 ? (gastado / presupuesto) * 100 : 0.0;
  }

  public boolean isEnCurso() {
    return enCurso;
  }
}
