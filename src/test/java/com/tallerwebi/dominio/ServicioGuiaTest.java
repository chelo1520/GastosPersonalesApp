package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

public class ServicioGuiaTest {

  @Test
  void guardaElProgresoEnLaBaseYEnLaSesion() {
    RepositorioUsuario usuarios = mock(RepositorioUsuario.class);
    ServicioGuia guia = new ServicioGuia(usuarios);
    Usuario usuario = new Usuario();
    usuario.setId(7L);

    guia.guardarPaso(usuario, 2);

    verify(usuarios).actualizarGuia(7L, 2);
    assertTrue(usuario.getGuiaPaso() == 2);
  }
}
