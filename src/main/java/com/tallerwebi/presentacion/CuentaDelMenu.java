package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import java.util.Locale;

/**
 * Lo que muestra el menú de la cuenta del navbar (rediseño v2): el nombre de la persona con su inicial
 * en un círculo (sin apellido, para simplificar). El nombre es opcional en el perfil: sin él se usa
 * el email.
 */
public final class CuentaDelMenu {

  private static final String SIN_DATOS = "?";

  private final String nombre;
  private final String email;
  private final String inicial;

  private CuentaDelMenu(String nombre, String email, String inicial) {
    this.nombre = nombre;
    this.email = email;
    this.inicial = inicial;
  }

  // La usa el navbar con el usuario de la sesión: T(...CuentaDelMenu).de(session.USUARIO)
  public static CuentaDelMenu de(Usuario usuario) {
    String email = usuario.getEmail() == null ? "" : usuario.getEmail();
    String nombre = textoOVacio(usuario.getNombre());
    String visible = nombre.isEmpty() ? email : nombre;
    return new CuentaDelMenu(visible, email, primeraLetra(visible));
  }

  private static String textoOVacio(String texto) {
    return texto == null ? "" : texto.trim();
  }

  private static String primeraLetra(String texto) {
    if (texto.isEmpty()) {
      return "";
    }
    return texto.substring(0, 1).toUpperCase(Locale.ROOT);
  }

  public String getNombre() {
    return nombre;
  }

  public String getEmail() {
    return email;
  }

  public String getInicial() {
    return inicial.isEmpty() ? SIN_DATOS : inicial;
  }
}
