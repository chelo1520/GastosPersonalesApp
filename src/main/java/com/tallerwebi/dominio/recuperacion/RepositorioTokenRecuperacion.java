package com.tallerwebi.dominio.recuperacion;

/** Tokens de los links para cambiar la contraseña. */
public interface RepositorioTokenRecuperacion {
  void guardar(TokenRecuperacion token);

  TokenRecuperacion buscarPorHash(String hash);

  void modificar(TokenRecuperacion token);
}
