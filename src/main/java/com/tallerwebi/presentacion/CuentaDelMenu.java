package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import java.util.Locale;

/**
 * Lo que muestra el menú de la cuenta del navbar (rediseño v2): en el botón, el círculo con las
 * iniciales de nombre y apellido y solo el nombre; al desplegar, nombre y apellido con el email.
 * Nombre y apellido son opcionales en el perfil: sin nombre se usa el email.
 */
public final class CuentaDelMenu {

  private static final String SIN_DATOS = "?";

  private final String nombre;
  private final String nombreCompleto;
  private final String email;
  private final String iniciales;

  private CuentaDelMenu(String nombre, String nombreCompleto, String email, String iniciales) {
    this.nombre = nombre;
    this.nombreCompleto = nombreCompleto;
    this.email = email;
    this.iniciales = iniciales;
  }

  // La usa el navbar con el usuario de la sesión: T(...CuentaDelMenu).de(session.USUARIO)
  public static CuentaDelMenu de(Usuario usuario) {
    String email = usuario.getEmail() == null ? "" : usuario.getEmail();
    String nombre = textoOVacio(usuario.getNombre());
    if (nombre.isEmpty()) {
      return new CuentaDelMenu(email, email, email, primeraLetra(email));
    }
    String apellido = textoOVacio(usuario.getApellido());
    String completo = apellido.isEmpty() ? nombre : nombre + " " + apellido;
    return new CuentaDelMenu(
      nombre,
      completo,
      email,
      primeraLetra(nombre) + primeraLetra(apellido)
    );
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

  // Botón del menú: solo el nombre (o el email)
  public String getNombre() {
    return nombre;
  }

  // Encabezado del menú desplegado: nombre y apellido (o el email)
  public String getNombreCompleto() {
    return nombreCompleto;
  }

  public String getEmail() {
    return email;
  }

  public String getIniciales() {
    return iniciales.isEmpty() ? SIN_DATOS : iniciales;
  }
}
