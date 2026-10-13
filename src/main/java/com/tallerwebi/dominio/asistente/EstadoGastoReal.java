package com.tallerwebi.dominio.asistente;

/** Semáforo del veredicto: cuánto creció el gasto por encima de la inflación. */
public enum EstadoGastoReal {
  VERDE("ok"),
  AMARILLO("warn"),
  ROJO("over");

  // Hasta acá se considera que el gasto real se mantuvo
  public static final double UMBRAL_AMARILLO = 0.0;
  public static final double UMBRAL_ROJO = 5.0;

  private final String estilo;

  EstadoGastoReal(String estilo) {
    this.estilo = estilo;
  }

  public static EstadoGastoReal de(double variacionRealPorcentaje) {
    if (variacionRealPorcentaje > UMBRAL_ROJO) {
      return ROJO;
    }
    if (variacionRealPorcentaje > UMBRAL_AMARILLO) {
      return AMARILLO;
    }
    return VERDE;
  }

  public String getEstilo() {
    return estilo;
  }
}
