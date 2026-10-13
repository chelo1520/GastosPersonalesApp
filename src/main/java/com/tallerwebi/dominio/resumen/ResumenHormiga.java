package com.tallerwebi.dominio.resumen;

import java.util.List;

/**
 * Gastos hormiga del mes: chicos, frecuentes y prescindibles, contra el presupuesto
 */
public class ResumenHormiga {

  private final Double total;
  private final Double porcentajePresupuesto;
  private final List<ResumenCategoria> categorias;

  public ResumenHormiga(
    Double total,
    Double porcentajePresupuesto,
    List<ResumenCategoria> categorias
  ) {
    this.total = total;
    this.porcentajePresupuesto = porcentajePresupuesto;
    this.categorias = categorias;
  }

  public Double getTotal() {
    return total;
  }

  public Double getPorcentajePresupuesto() {
    return porcentajePresupuesto;
  }

  public List<ResumenCategoria> getCategorias() {
    return categorias;
  }
}
