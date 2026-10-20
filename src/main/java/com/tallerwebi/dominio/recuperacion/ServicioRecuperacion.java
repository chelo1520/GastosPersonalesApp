package com.tallerwebi.dominio.recuperacion;

/** "¿Olvidaste tu contraseña?": manda el link por mail y cambia la contraseña con él. */
public interface ServicioRecuperacion {
  // No avisa si el email no está registrado: así no se puede averiguar quién tiene cuenta
  void solicitar(String email);

  boolean esValido(String token);

  void restablecer(String token, String passwordNueva);
}
