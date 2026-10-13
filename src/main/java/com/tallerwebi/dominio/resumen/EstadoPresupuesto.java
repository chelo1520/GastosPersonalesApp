package com.tallerwebi.dominio.resumen;

/**
 * Semáforo de una categoría según cuánto del presupuesto se usó.
 * Umbrales únicos para el resumen, presupuestos y las alertas (HU-05).
 */
public enum EstadoPresupuesto {
  VERDE("ok"),
  AMARILLO("warn"),
  ROJO("over"),
  SIN_PRESUPUESTO("none");

  public static final double UMBRAL_AMARILLO = 80.0;
  public static final double UMBRAL_ROJO = 100.0;

  // Sufijo de la clase CSS del chip / barra (is-ok, is-warn, is-over)
  private final String estilo;

  EstadoPresupuesto(String estilo) {
    this.estilo = estilo;
  }

  public static EstadoPresupuesto de(Double porcentajeUsado) {
    if (porcentajeUsado == null) {
      return SIN_PRESUPUESTO;
    }
    if (porcentajeUsado >= UMBRAL_ROJO) {
      return ROJO;
    }
    if (porcentajeUsado >= UMBRAL_AMARILLO) {
      return AMARILLO;
    }
    return VERDE;
  }

  public String getEstilo() {
    return estilo;
  }
}
