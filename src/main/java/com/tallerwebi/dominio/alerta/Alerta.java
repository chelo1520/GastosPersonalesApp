package com.tallerwebi.dominio.alerta;

/**
 * Aviso sobre gastos hormiga o presupuestos: informa cuánto se va, no bloquea nada
 */
public class Alerta {

  /** Qué mira la alerta: el total hormiga del mes, un gasto que se repite o el presupuesto de una categoría */
  public enum Tipo {
    ACUMULADO,
    REPETICION,
    PRESUPUESTO,
  }

  /** Amarilla avisa, roja marca que ya se pasó el límite */
  public enum Nivel {
    AMARILLA,
    ROJA,
  }

  private final Tipo tipo;
  private final Nivel nivel;
  private final String mensaje;
  // Categoría que se puede marcar como "Quitar categoría de hormiga" desde la alerta (solo en la de repetición)
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
