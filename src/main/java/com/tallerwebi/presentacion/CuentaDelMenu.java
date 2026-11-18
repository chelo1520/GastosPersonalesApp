package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import java.util.Locale;

/**
 * Lo que muestra el menú de la cuenta del navbar (rediseño v2): "Nombre Apellido" con sus iniciales
 * en un círculo. Nombre y apellido son opcionales en el perfil: sin ellos se usa el email.
 */
public final class CuentaDelMenu {

  private static final String SIN_DATOS = "?";

  private final String nombre;
  private final String email;
  private final String iniciales;

  private CuentaDelMenu(String nombre, String email, String iniciales) {
    this.nombre = nombre;
    this.email = email;
    this.iniciales = iniciales;
  }

  // La usa el navbar con el usuario de la sesión: T(...CuentaDelMenu).de(session.USUARIO)
  public static CuentaDelMenu de(Usuario usuario) {
    String email = usuario.getEmail() == null ? "" : usuario.getEmail();
    String nombre = textoOVacio(usuario.getNombre());
    String apellido = textoOVacio(usuario.getApellido());
    String completo = (nombre + " " + apellido).trim();
    if (completo.isEmpty()) {
      return new CuentaDelMenu(email, email, inicial(email));
    }
    return new CuentaDelMenu(completo, email, inicial(nombre) + inicial(apellido));
  }

  private static String textoOVacio(String texto) {
    return texto == null ? "" : texto.trim();
  }

  private static String inicial(String texto) {
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

  public String getIniciales() {
    return iniciales.isEmpty() ? SIN_DATOS : iniciales;
  }
}
