package com.tallerwebi.dominio.resumen;

import java.util.List;

/**
 * Gastos hormiga del mes: chicos, frecuentes y prescindibles, contra el presupuesto
 */
public class ResumenHormiga {

  private final Double total;
  private final Double porcentajePresupuesto;
  private final List<ResumenCategoria> categorias;
  // Cuántos gastos chicos suman el total: "N compras chicas"
  private final int cantidad;

  public ResumenHormiga(
    Double total,
    Double porcentajePresupuesto,
    List<ResumenCategoria> categorias
  ) {
    this(total, porcentajePresupuesto, categorias, 0);
  }

  public ResumenHormiga(
    Double total,
    Double porcentajePresupuesto,
    List<ResumenCategoria> categorias,
    int cantidad
  ) {
    this.total = total;
    this.porcentajePresupuesto = porcentajePresupuesto;
    this.categorias = categorias;
    this.cantidad = cantidad;
  }

  public int getCantidad() {
    return cantidad;
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
