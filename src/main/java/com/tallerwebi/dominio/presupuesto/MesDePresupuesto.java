package com.tallerwebi.dominio.presupuesto;

import java.time.YearMonth;

/**
 * Un mes en la vista anual del presupuesto: su total efectivo (propio o el del perfil) y lo que ya
 * se asignó a categorías.
 *
 * @param mes el mes
 * @param total total del mes: el definido para ese mes o, si no hay, el del perfil
 * @param propio true si el mes tiene un total definido aparte del perfil
 * @param asignado suma de lo asignado a categorías ese mes
 */
public record MesDePresupuesto(YearMonth mes, double total, boolean propio, double asignado) {
  public double sinAsignar() {
    return total - asignado;
  }
}
