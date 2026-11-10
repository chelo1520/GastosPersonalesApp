package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tallerwebi.dominio.ServicioGuia;
import com.tallerwebi.dominio.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorGuiaTest {

  private ServicioGuia guia;
  private ControladorGuia controlador;
  private MockHttpServletRequest request;
  private Usuario usuario;
  private RedirectAttributesModelMap mensajes;

  @BeforeEach
  void preparar() {
    guia = mock(ServicioGuia.class);
    controlador = new ControladorGuia(guia);
    request = new MockHttpServletRequest();
    usuario = new Usuario();
    request.getSession().setAttribute("USUARIO", usuario);
    mensajes = new RedirectAttributesModelMap();
  }

  @Test
  void empiezaLaGuiaYVaAlPerfil() {
    usuario.setGuiaPaso(0);

    assertEquals("redirect:/perfil", controlador.cambiar("continuar", request, mensajes));
    verify(guia).guardarPaso(usuario, 1);
  }

  @Test
  void avanzaSinExigirQueSeCompletenLosDatos() {
    usuario.setGuiaPaso(1);
    assertEquals("redirect:/presupuestos", controlador.cambiar("continuar", request, mensajes));
    verify(guia).guardarPaso(usuario, 2);
  }

  @Test
  void permiteVolverAlPasoAnterior() {
    usuario.setGuiaPaso(2);
    assertEquals("redirect:/perfil", controlador.cambiar("anterior", request, mensajes));
    verify(guia).guardarPaso(usuario, 1);
  }

  @Test
  void soloPuedePosponerLaBienvenida() {
    usuario.setGuiaPaso(0);
    assertEquals("redirect:/home", controlador.cambiar("pausar", request, mensajes));
    assertEquals(true, request.getSession().getAttribute("GUIA_POSPUESTA"));
    verify(guia, never()).guardarPaso(usuario, -1);
    usuario.setGuiaPaso(1);
    assertThrows(
      ResponseStatusException.class,
      () -> controlador.cambiar("pausar", request, mensajes)
    );
    assertThrows(
      ResponseStatusException.class,
      () -> controlador.cambiar("omitir", request, mensajes)
    );
  }

  @Test
  void sinSesionNoModificaElProgreso() {
    request.getSession().invalidate();
    assertEquals("redirect:/login", controlador.cambiar("continuar", request, mensajes));
  }
}
