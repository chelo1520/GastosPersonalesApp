package com.tallerwebi.dominio.asistente;

/** Qué explica la variación de una categoría: el precio o el comportamiento. */
public enum Diagnostico {
  CONSUMIS_MAS("Consumís más", "over"),
  SUBIO_EL_PRECIO("Subió el precio", "warn"),
  ESTABLE("Estable", "ok"),
  AHORRASTE("Ahorraste", "ok"),
  NUEVO("Nuevo este mes", "none"),
  SIN_COMPARACION("Sin comparación", "none");

  // Variación real (en puntos porcentuales) a partir de la cual se considera un cambio de hábito
  public static final double UMBRAL = 5.0;

  private final String texto;
  private final String estilo;

  Diagnostico(String texto, String estilo) {
    this.texto = texto;
    this.estilo = estilo;
  }

  public static Diagnostico de(Double variacionNominal, Double variacionReal) {
    if (variacionNominal == null || variacionReal == null) {
      return SIN_COMPARACION;
    }
    if (variacionReal > UMBRAL) {
      return CONSUMIS_MAS;
    }
    if (variacionReal < -UMBRAL) {
      return AHORRASTE;
    }
    return variacionNominal > 0 ? SUBIO_EL_PRECIO : ESTABLE;
  }

  public String getTexto() {
    return texto;
  }

  public String getEstilo() {
    return estilo;
  }
}
