package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorCategoriaTest {

  private CategoriaRepositorio categoriaRepositorio;
  private ControladorCategoria controladorCategoria;
  private HttpServletRequest request;
  private HttpSession session;
  private Categoria cafe;

  @BeforeEach
  public void init() {
    categoriaRepositorio = mock(CategoriaRepositorio.class);
    controladorCategoria = new ControladorCategoria(categoriaRepositorio);

    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("USUARIO")).thenReturn(mock(Usuario.class));

    cafe = new Categoria();
    cafe.setId(7L);
    cafe.setEsencial(false);
    when(categoriaRepositorio.buscarPorId(7L)).thenReturn(cafe);
  }

  @Test
  public void debeMarcarLaCategoriaComoEsencialYVolverALaPantallaPedida() {
    ModelAndView resultado = controladorCategoria.marcarComoNoHormiga(
      7L,
      "/mostrar-gastos",
      request
    );

    assertTrue(cafe.isEsencial());
    verify(categoriaRepositorio).modificar(cafe);
    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
  }

  @Test
  public void debeMarcarLaCategoriaComoPrescindibleCuandoSeTocaEsHormiga() {
    cafe.setEsencial(true);

    ModelAndView resultado = controladorCategoria.marcarComoHormiga(7L, "/mostrar-gastos", request);

    assertFalse(cafe.isEsencial());
    verify(categoriaRepositorio).modificar(cafe);
    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
  }

  @Test
  public void noDebeMarcarComoHormigaUnaCategoriaFija() {
    Categoria salud = new Categoria();
    salud.setId(3L);
    salud.setFija(true);
    when(categoriaRepositorio.buscarPorId(3L)).thenReturn(salud);

    ModelAndView resultado = controladorCategoria.marcarComoHormiga(3L, "/mostrar-gastos", request);

    assertTrue(salud.isEsencial());
    verify(categoriaRepositorio, never()).modificar(any());
    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
  }

  @Test
  public void debeRedirigirALoginSinTocarLaCategoriaCuandoSeTocaEsHormigaSinSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorCategoria.marcarComoHormiga(7L, "/home", request);

    assertEquals("redirect:/login", resultado.getViewName());
    verifyNoInteractions(categoriaRepositorio);
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
  public void noDebeModificarNadaCuandoLaCategoriaNoExiste() {
    ModelAndView resultado = controladorCategoria.marcarComoNoHormiga(99L, "/home", request);

    verify(categoriaRepositorio, never()).modificar(any());
    assertEquals("redirect:/home", resultado.getViewName());
  }

  @Test
  public void debeRedirigirALoginSinTocarLaCategoriaCuandoNoHayUsuarioEnSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorCategoria.marcarComoNoHormiga(7L, "/home", request);

    assertEquals("redirect:/login", resultado.getViewName());
    verifyNoInteractions(categoriaRepositorio);
  }
}
