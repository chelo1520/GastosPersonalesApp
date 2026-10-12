package com.tallerwebi.dominio.resumen;

/** Gastos mensuales agrupados por categoría. */
public class ResumenCategoria {

  private final String nombre;
  private final Double gastado;
  private final Double porcentajeTotal;

  public ResumenCategoria(String nombre, Double gastado, Double porcentajeTotal) {
    this.nombre = nombre;
    this.gastado = gastado;
    this.porcentajeTotal = porcentajeTotal;
  }

  public String getNombre() {
    return nombre;
  }

  public Double getGastado() {
    return gastado;
  }

  public Double getPorcentajeTotal() {
    return porcentajeTotal;
  }
}
