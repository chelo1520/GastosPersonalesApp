package com.tallerwebi.dominio.asistente;

import java.util.List;

/**
 * Respuesta estructurada del asistente: los consejos en orden de importancia, cada uno con su
 * id y el texto redactado. El modelo no puede agregar consejos ni cambiar montos: solo ids
 * conocidos y texto.
 */
public record RedaccionDeConsejos(List<ConsejoRedactado> consejos) {
  public record ConsejoRedactado(Long id, String texto) {}
}
