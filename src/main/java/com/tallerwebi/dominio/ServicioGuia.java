package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/** Controla el recorrido inicial de cada cuenta. */
@Service
@Transactional
public class ServicioGuia {

  private final RepositorioUsuario usuarios;

  public ServicioGuia(RepositorioUsuario usuarios) {
    this.usuarios = usuarios;
  }

  public void guardarPaso(Usuario usuario, int paso) {
    usuarios.actualizarGuia(usuario.getId(), paso);
    usuario.setGuiaPaso(paso);
  }
}
