package com.tallerwebi.dominio.resumen;

import java.util.List;

/**
 * Gastos hormiga del mes: chicos, frecuentes y prescindibles, contra el presupuesto
 */
public class ResumenHormiga {

  private final Double total;
  private final Double porcentajePresupuesto;
  private final Double proyeccion;
  private final Double porcentajeProyeccion;
  private final List<ResumenCategoria> categorias;

  public ResumenHormiga(
    Double total,
    Double porcentajePresupuesto,
    Double proyeccion,
    Double porcentajeProyeccion,
    List<ResumenCategoria> categorias
  ) {
    this.total = total;
    this.porcentajePresupuesto = porcentajePresupuesto;
    this.proyeccion = proyeccion;
    this.porcentajeProyeccion = porcentajeProyeccion;
    this.categorias = categorias;
  }

  public Double getTotal() {
    return total;
  }

  public Double getPorcentajePresupuesto() {
    return porcentajePresupuesto;
  }

  public Double getProyeccion() {
    return proyeccion;
  }

  public Double getPorcentajeProyeccion() {
    return porcentajeProyeccion;
  }

  public List<ResumenCategoria> getCategorias() {
    return categorias;
  }
}
