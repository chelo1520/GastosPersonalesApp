package com.tallerwebi.dominio;

/** Lo que el usuario hace con su propia cuenta desde el perfil. */
public interface ServicioCuenta {
  // Devuelve el usuario con la contraseña nueva, para actualizar la sesión
  Usuario cambiarContrasena(Usuario usuario, String actual, String nueva);

  // Borra la cuenta con sus gastos, presupuestos, consejos y links de recuperación
  void eliminarCuenta(Usuario usuario, String password);
}
