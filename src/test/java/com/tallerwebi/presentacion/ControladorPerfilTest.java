package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPerfilTest {

  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private Usuario usuarioMock;
  private ControladorPerfil controladorPerfil;

  @BeforeEach
  public void init() {
    this.controladorPerfil = new ControladorPerfil();
    this.usuarioMock = mock(Usuario.class);
    this.requestMock = mock(HttpServletRequest.class);
    this.sessionMock = mock(HttpSession.class);
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("USUARIO")).thenReturn(usuarioMock);
  }

  @Test
  public void cuandoHayUnUsuarioAuntenticadoEnLaSesionMuestraElPerfilYRetornaUsuarioALaVista() {
    ModelAndView resultado = controladorPerfil.mostrarPerfil(requestMock);

    assertEquals("perfil", resultado.getViewName());
    assertSame(usuarioMock, resultado.getModel().get("usuario"));
  }

  @Test
  public void cuandoNoHayUsuarioEnLaSesionRedirigeAlLogin() {
    when(sessionMock.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorPerfil.mostrarPerfil(requestMock);

    assertEquals("redirect:/login", resultado.getViewName());
  }

  @Test
  public void cuandoNoExisteLaSesionRedirigeAlLogin() {
    when(requestMock.getSession(false)).thenReturn(null);

    ModelAndView resultado = controladorPerfil.mostrarPerfil(requestMock);

    assertEquals("redirect:/login", resultado.getViewName());
  }
}
