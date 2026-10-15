package com.tallerwebi.dominio.resumen;

/** Gastos mensuales de una categoría contra su presupuesto. */
public final class ResumenCategoria {

  private final String nombre;
  private final Double gastado;
  private final Double porcentajeTotal;
  private final Double presupuesto;
  private final EstadoPresupuesto estado;
  // Para el botón "Quitar categoría de hormiga" y el check "¿Es hormiga?"; null sin categoría
  private final Long categoriaId;
  // Check "¿Es hormiga?" de la tabla: null cuando no se puede cambiar (fija o sin categoría)
  private final Boolean esHormiga;

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
    this(nombre, gastado, porcentajeTotal, presupuesto, categoriaId, null);
  }

  public ResumenCategoria(
    String nombre,
    Double gastado,
    Double porcentajeTotal,
    Double presupuesto,
    Long categoriaId,
    Boolean esHormiga
  ) {
    this.nombre = nombre;
    this.gastado = gastado;
    this.porcentajeTotal = porcentajeTotal;
    this.presupuesto = presupuesto;
    this.categoriaId = categoriaId;
    this.esHormiga = esHormiga;
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

  public Boolean getEsHormiga() {
    return esHormiga;
  }
}
