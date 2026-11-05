package com.tallerwebi.dominio.categoria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qué categorías son prescindibles para un usuario: su preferencia si la marcó, y si no, lo que
 * dice la categoría del maestro. Una categoría fija es esencial para todos.
 */
public class MarcasHormiga {

  private final Map<Long, Boolean> esencialPorCategoria;

  private MarcasHormiga(Map<Long, Boolean> esencialPorCategoria) {
    this.esencialPorCategoria = esencialPorCategoria;
  }

  /** Sin preferencias: vale lo que dice cada categoría. */
  public static MarcasHormiga porDefecto() {
    return new MarcasHormiga(new HashMap<>());
  }

  public static MarcasHormiga de(List<PreferenciaCategoria> preferencias) {
    Map<Long, Boolean> esencialPorCategoria = new HashMap<>();
    for (PreferenciaCategoria preferencia : preferencias) {
      if (preferencia.getCategoria() != null && preferencia.getEsencial() != null) {
        esencialPorCategoria.put(preferencia.getCategoria().getId(), preferencia.getEsencial());
      }
    }
    return new MarcasHormiga(esencialPorCategoria);
  }

  // Sin categoría se trata como esencial: ante la duda, no es hormiga
  public boolean esPrescindible(Categoria categoria) {
    if (categoria == null || categoria.isFija()) {
      return false;
    }
    // Una categoría sin id (todavía no guardada) no puede tener preferencia
    Boolean preferencia = categoria.getId() == null
      ? null
      : esencialPorCategoria.get(categoria.getId());
    return preferencia == null ? !categoria.isEsencial() : !preferencia;
  }
}
