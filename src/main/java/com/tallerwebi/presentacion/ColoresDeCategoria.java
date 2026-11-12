package com.tallerwebi.presentacion;

import java.util.Map;

/**
 * Color de cada categoría del maestro, según la paleta del rediseño v2 (diseno/v2 del coord). Una
 * categoría que no esté en la lista usa el gris de "Otros".
 */
public final class ColoresDeCategoria {

  private static final String OTROS = "#8a8580";
  private static final Map<String, String> COLORES = Map.ofEntries(
    Map.entry("Vivienda", "#0f4c47"),
    Map.entry("Servicios", "#2f9384"),
    Map.entry("Alimentación", "#7fc4b8"),
    Map.entry("Transporte", "#d4a72c"),
    Map.entry("Delivery", "#c2510e"),
    Map.entry("Kiosco", "#a39d96"),
    Map.entry("Café y snacks", "#1f5f58"),
    Map.entry("Entretenimiento", "#3aa597"),
    Map.entry("Comida afuera", "#b8863b"),
    Map.entry("Educación", "#4f7c74"),
    Map.entry("Estética y cuidado personal", "#8fb3ab"),
    Map.entry("Ocio", "#6f8f62"),
    Map.entry("Salud", "#5b8f6a"),
    Map.entry("Suscripciones", "#4a6fa5"),
    Map.entry("Taxis y apps", "#b56b2c"),
    Map.entry("Otros", OTROS)
  );

  private ColoresDeCategoria() {}

  public static String de(String nombre) {
    return nombre == null ? OTROS : COLORES.getOrDefault(nombre, OTROS);
  }
}
