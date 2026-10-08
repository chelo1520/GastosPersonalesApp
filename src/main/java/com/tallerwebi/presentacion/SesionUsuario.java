package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Lee el usuario logueado de la sesión, compartido por los controladores
 */
final class SesionUsuario {

  private SesionUsuario() {}

  // null si no hay sesión o si la sesión no tiene un usuario
  static Usuario obtener(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }

    Object usuario = session.getAttribute("USUARIO");
    return usuario instanceof Usuario ? (Usuario) usuario : null;
  }
}
