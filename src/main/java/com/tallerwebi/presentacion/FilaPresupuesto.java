package com.tallerwebi.presentacion;

/**
 * Una categoría en la tabla de presupuestos: con monto definido o "sin presupuesto"
 */
public class FilaPresupuesto {

  private final String categoria;
  private final Double monto;

  public FilaPresupuesto(String categoria, Double monto) {
    this.categoria = categoria;
    this.monto = monto;
  }

  public String getCategoria() {
    return categoria;
  }

  // null cuando la categoría no tiene presupuesto en el mes
  public Double getMonto() {
    return monto;
  }

  public boolean isDefinido() {
    return monto != null;
  }
}
