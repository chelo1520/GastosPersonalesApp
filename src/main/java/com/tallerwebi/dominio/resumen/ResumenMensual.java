package com.tallerwebi.dominio.resumen;

import java.util.List;

/**
 * Totales de un mes: lo gastado contra el presupuesto, en total, por categoría y en gastos hormiga
 */
public class ResumenMensual {

  private final Double gastado;
  private final Double presupuesto;
  private final Double disponible;
  private final Double porcentajeUsado;
  private final List<ResumenCategoria> categorias;
  private final ResumenHormiga hormiga;

  public ResumenMensual(
    Double gastado,
    Double presupuesto,
    Double disponible,
    Double porcentajeUsado,
    List<ResumenCategoria> categorias,
    ResumenHormiga hormiga
  ) {
    this.gastado = gastado;
    this.presupuesto = presupuesto;
    this.disponible = disponible;
    this.porcentajeUsado = porcentajeUsado;
    this.categorias = categorias;
    this.hormiga = hormiga;
  }

  public Double getGastado() {
    return gastado;
  }

  public Double getPresupuesto() {
    return presupuesto;
  }

  public Double getDisponible() {
    return disponible;
  }

  public Double getPorcentajeUsado() {
    return porcentajeUsado;
  }

  public List<ResumenCategoria> getCategorias() {
    return categorias;
  }

  public ResumenHormiga getHormiga() {
    return hormiga;
  }
}
