package com.tallerwebi.dominio;

/**
 * Lo que tiene que cumplir una contraseña nueva (registro y cambio de contraseña). Son las mismas
 * reglas que marca en verde el formulario (login_funciones.js).
 */
public final class ReglasDeContrasena {

  public static final int LARGO_MINIMO = 6;
  public static final String MENSAJE =
    "La contraseña tiene que tener al menos " +
    LARGO_MINIMO +
    " caracteres, una mayúscula, una minúscula y un carácter especial";

  private ReglasDeContrasena() {}

  public static boolean cumple(String password) {
    return (
      password != null &&
      password.length() >= LARGO_MINIMO &&
      password.chars().anyMatch(Character::isUpperCase) &&
      password.chars().anyMatch(Character::isLowerCase) &&
      password.chars().anyMatch(ReglasDeContrasena::esEspecial)
    );
  }

  // Especial: ni letra (con o sin tilde), ni número, ni espacio. Ej.: ! @ # $ % & * . - _
  private static boolean esEspecial(int caracter) {
    return !Character.isLetterOrDigit(caracter) && !Character.isWhitespace(caracter);
  }
}
