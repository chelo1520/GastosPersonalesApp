package com.tallerwebi.dominio.resumen;

/** Gastos mensuales de una categoría contra su presupuesto. */
public final class ResumenCategoria {

  private final String nombre;
  private final Double gastado;
  private final Double porcentajeTotal;
  private final Double presupuesto;
  private final EstadoPresupuesto estado;
  // Para el botón "No es hormiga" de la tarjeta de gastos hormiga; null en el resto
  private final Long categoriaId;

  // Sin presupuesto definido para la categoría
  public ResumenCategoria(String nombre, Double gastado, Double porcentajeTotal) {
    this(nombre, gastado, porcentajeTotal, null);
  }

  public ResumenCategoria(
    String nombre,
    Double gastado,
    Double porcentajeTotal,
    Double presupuesto
  ) {
    this(nombre, gastado, porcentajeTotal, presupuesto, null);
  }

  public ResumenCategoria(
    String nombre,
    Double gastado,
    Double porcentajeTotal,
    Double presupuesto,
    Long categoriaId
  ) {
    this.nombre = nombre;
    this.gastado = gastado;
    this.porcentajeTotal = porcentajeTotal;
    this.presupuesto = presupuesto;
    this.categoriaId = categoriaId;
    this.estado = EstadoPresupuesto.de(getPorcentajeUsado());
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

  // null cuando la categoría no tiene presupuesto en el mes; lo mismo disponible y porcentajeUsado
  public Double getPresupuesto() {
    return presupuesto;
  }

  public Double getDisponible() {
    if (!isTienePresupuesto()) {
      return null;
    }
    return presupuesto - gastado;
  }

  public Double getPorcentajeUsado() {
    if (!isTienePresupuesto()) {
      return null;
    }
    return (gastado / presupuesto) * 100;
  }

  public EstadoPresupuesto getEstado() {
    return estado;
  }

  public boolean isTienePresupuesto() {
    return presupuesto != null && presupuesto > 0;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }
}
