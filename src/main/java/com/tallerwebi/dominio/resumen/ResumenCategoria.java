package com.tallerwebi.dominio.resumen;

/** Gastos mensuales agrupados por categoría. */
public class ResumenCategoria {

  private final String nombre;
  private final Double gastado;
  private final Double porcentajeTotal;
  private final Long categoriaId;

  public ResumenCategoria(String nombre, Double gastado, Double porcentajeTotal) {
    this(nombre, gastado, porcentajeTotal, null);
  }

  public ResumenCategoria(String nombre, Double gastado, Double porcentajeTotal, Long categoriaId) {
    this.nombre = nombre;
    this.gastado = gastado;
    this.porcentajeTotal = porcentajeTotal;
    this.categoriaId = categoriaId;
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

  public Long getCategoriaId() {
    return categoriaId;
  }
}
