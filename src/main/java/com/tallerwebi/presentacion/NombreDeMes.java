package com.tallerwebi.presentacion;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Nombre de un mes para mostrar en pantalla, ej. "Octubre 2026" o, corto, "Oct 2026"
 */
final class NombreDeMes {

  private static final Locale ARGENTINA = Locale.of("es", "AR");

  private NombreDeMes() {}

  static String de(YearMonth mes) {
    return nombre(mes, TextStyle.FULL) + " " + mes.getYear();
  }

  static String corto(YearMonth mes) {
    return nombre(mes, TextStyle.SHORT).replace(".", "") + " " + mes.getYear();
  }

  private static String nombre(YearMonth mes, TextStyle estilo) {
    String nombre = mes.getMonth().getDisplayName(estilo, ARGENTINA);
    return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
  }
}
