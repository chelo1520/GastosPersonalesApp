package com.tallerwebi.dominio.alerta;

/**
 * Aviso sobre gastos hormiga: informa cuánto se va, no bloquea nada
 */
public class Alerta {

  /** Qué mira la alerta: el total del mes o un gasto que se repite */
  public enum Tipo {
    ACUMULADO,
    REPETICION,
  }

  /** Amarilla avisa, roja marca que ya se pasó el 10 % del presupuesto */
  public enum Nivel {
    AMARILLA,
    ROJA,
  }

  private final Tipo tipo;
  private final Nivel nivel;
  private final String mensaje;
  // Categoría que se puede marcar como "No es hormiga" desde la alerta (solo en la de repetición)
  private final Long categoriaId;

  public Alerta(Tipo tipo, Nivel nivel, String mensaje, Long categoriaId) {
    this.tipo = tipo;
    this.nivel = nivel;
    this.mensaje = mensaje;
    this.categoriaId = categoriaId;
  }

  public Tipo getTipo() {
    return tipo;
  }

  public Nivel getNivel() {
    return nivel;
  }

  public String getMensaje() {
    return mensaje;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }
}
