package com.tallerwebi.presentacion;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Nombre de un mes para mostrar en pantalla, ej. "Octubre 2026"
 */
final class NombreDeMes {

  private NombreDeMes() {}

  static String de(YearMonth mes) {
    String nombre = mes.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "AR"));
    return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + mes.getYear();
  }
}
