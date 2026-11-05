package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.ServicioCategoria;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorCategoriaTest {

  private ServicioCategoria servicioCategoria;
  private ControladorCategoria controladorCategoria;
  private HttpServletRequest request;
  private HttpSession session;
  private Usuario usuario;

  @BeforeEach
  public void init() {
    servicioCategoria = mock(ServicioCategoria.class);
    controladorCategoria = new ControladorCategoria(servicioCategoria);
    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    usuario = new Usuario();
    usuario.setEmail("medio@gastitos.demo");
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("USUARIO")).thenReturn(usuario);
  }

  @Test
  public void debeQuitarLaCategoriaDeHormigaParaElUsuarioYVolverALaPantallaPedida() {
    ModelAndView resultado = controladorCategoria.marcarComoNoHormiga(
      7L,
      "/mostrar-gastos",
      request
    );

    verify(servicioCategoria).quitarDeHormiga(usuario, 7L);
    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
  }

  @Test
  public void debeMarcarLaCategoriaComoHormigaParaElUsuarioCuandoSeTocaEsHormiga() {
    ModelAndView resultado = controladorCategoria.marcarComoHormiga(7L, "/mostrar-gastos", request);

    verify(servicioCategoria).marcarComoHormiga(usuario, 7L);
    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
  }

  @Test
  public void debeRedirigirALoginSinTocarNadaCuandoSeTocaEsHormigaSinSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorCategoria.marcarComoHormiga(7L, "/home", request);

    assertEquals("redirect:/login", resultado.getViewName());
    verifyNoInteractions(servicioCategoria);
  }

  @Test
  public void debeRedirigirALoginSinTocarNadaCuandoSeQuitaDeHormigaSinSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorCategoria.marcarComoNoHormiga(7L, "/home", request);

    assertEquals("redirect:/login", resultado.getViewName());
    verifyNoInteractions(servicioCategoria);
  }

  @Test
  public void debeVolverAlResumenCuandoLaPantallaPedidaNoEsDeLaApp() {
    ModelAndView resultado = controladorCategoria.marcarComoNoHormiga(
      7L,
      "https://otro-sitio.com",
      request
    );

    assertEquals("redirect:/home", resultado.getViewName());
  }

  @Test
  public void debeVolverAlMismoMesDelResumenCuandoSeTildaElCheck() {
    ModelAndView resultado = controladorCategoria.marcarComoHormiga(
      7L,
      "/home?mes=2026-09",
      request
    );

    assertEquals("redirect:/home?mes=2026-09", resultado.getViewName());
  }

  @Test
  public void debeVolverAlResumenCuandoElMesPedidoNoTieneFormatoDeMes() {
    ModelAndView resultado = controladorCategoria.marcarComoHormiga(
      7L,
      "/home?mes=2026-09&x=https://otro-sitio.com",
      request
    );

    assertEquals("redirect:/home", resultado.getViewName());
  }

  @Test
  public void debeVolverAlResumenCuandoNoSePideNingunaPantalla() {
    ModelAndView resultado = controladorCategoria.marcarComoNoHormiga(7L, null, request);

    verify(servicioCategoria).quitarDeHormiga(usuario, 7L);
    assertEquals("redirect:/home", resultado.getViewName());
  }
}
