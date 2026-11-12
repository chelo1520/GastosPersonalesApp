package com.tallerwebi.dominio;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Nombre de un mes para mostrar en pantalla, ej. "Octubre 2026" o, corto, "Oct 2026"
 */
public final class NombreDeMes {

  private static final Locale ARGENTINA = Locale.of("es", "AR");

  private NombreDeMes() {}

  public static String de(YearMonth mes) {
    return nombre(mes, TextStyle.FULL) + " " + mes.getYear();
  }

  // "Octubre", sin el año: para listas donde el año ya está en el encabezado
  public static String sinAnio(YearMonth mes) {
    return nombre(mes, TextStyle.FULL);
  }

  public static String corto(YearMonth mes) {
    return nombre(mes, TextStyle.SHORT).replace(".", "") + " " + mes.getYear();
  }

  private static String nombre(YearMonth mes, TextStyle estilo) {
    String nombre = mes.getMonth().getDisplayName(estilo, ARGENTINA);
    return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
  }
}
