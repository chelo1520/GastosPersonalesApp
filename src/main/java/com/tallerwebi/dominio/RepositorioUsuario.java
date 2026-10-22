package com.tallerwebi.dominio;

public interface RepositorioUsuario {
  Usuario buscarUsuario(String email, String password);
  void guardar(Usuario usuario);
  Usuario buscar(String email);
  void modificar(Usuario usuario);

  // Junto con todo lo que es suyo: gastos, presupuestos, consejos y links de recuperación
  void eliminar(Usuario usuario);
}
