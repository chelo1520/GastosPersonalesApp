package com.tallerwebi.dominio.resumen;

/**
 * Lo gastado en una categoría durante un mes, comparado con su presupuesto
 */
public class ResumenCategoria {

  private final String nombre;
  private final Double gastado;
  private final Double porcentajeTotal;
  private final Double disponible;
  private final Double porcentajeUsado;

  public ResumenCategoria(
    String nombre,
    Double gastado,
    Double porcentajeTotal,
    Double disponible,
    Double porcentajeUsado
  ) {
    this.nombre = nombre;
    this.gastado = gastado;
    this.porcentajeTotal = porcentajeTotal;
    this.disponible = disponible;
    this.porcentajeUsado = porcentajeUsado;
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

  public Double getDisponible() {
    return disponible;
  }

  public Double getPorcentajeUsado() {
    return porcentajeUsado;
  }
}
