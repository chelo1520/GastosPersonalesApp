package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ReglasDeContrasena;
import com.tallerwebi.dominio.ServicioCuenta;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.ContrasenaIncorrecta;
import com.tallerwebi.dominio.excepcion.ContrasenaInvalida;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPerfilTest {

  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private Usuario usuarioMock;
  private ServicioCuenta servicioCuentaMock;
  private ControladorPerfil controladorPerfil;

  @BeforeEach
  public void init() {
    this.servicioCuentaMock = mock(ServicioCuenta.class);
    this.controladorPerfil = new ControladorPerfil(servicioCuentaMock);
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

  @Test
  public void debeCambiarLaContrasenaActualizarLaSesionYVolverAlPerfilConElAviso() {
    Usuario actualizado = new Usuario();
    when(servicioCuentaMock.cambiarContrasena(usuarioMock, "Actual1!", "Nueva123!"))
      .thenReturn(actualizado);

    ModelAndView resultado = controladorPerfil.cambiarContrasena(
      "Actual1!",
      "Nueva123!",
      "Nueva123!",
      requestMock
    );

    assertEquals("redirect:/perfil?contrasenaCambiada", resultado.getViewName());
    verify(sessionMock).setAttribute("USUARIO", actualizado);
  }

  @Test
  public void noDebeCambiarlaCuandoLasContrasenasNuevasNoCoinciden() {
    ModelAndView resultado = controladorPerfil.cambiarContrasena(
      "Actual1!",
      "Nueva123!",
      "Otra1234!",
      requestMock
    );

    assertEquals("perfil", resultado.getViewName());
    assertEquals(
      "Las contraseñas nuevas no coinciden",
      resultado.getModel().get("errorContrasena")
    );
    verify(servicioCuentaMock, never()).cambiarContrasena(any(), any(), any());
  }

  @Test
  public void debeAvisarCuandoLaContrasenaActualEsIncorrecta() {
    when(servicioCuentaMock.cambiarContrasena(usuarioMock, "Mal1234!", "Nueva123!"))
      .thenThrow(new ContrasenaIncorrecta());

    ModelAndView resultado = controladorPerfil.cambiarContrasena(
      "Mal1234!",
      "Nueva123!",
      "Nueva123!",
      requestMock
    );

    assertEquals("perfil", resultado.getViewName());
    assertEquals(
      "La contraseña actual no es correcta",
      resultado.getModel().get("errorContrasena")
    );
    verify(sessionMock, never()).setAttribute(any(), any());
  }

  @Test
  public void debeAvisarLasReglasCuandoLaNuevaNoLasCumple() {
    when(servicioCuentaMock.cambiarContrasena(usuarioMock, "Actual1!", "nueva"))
      .thenThrow(new ContrasenaInvalida());

    ModelAndView resultado = controladorPerfil.cambiarContrasena(
      "Actual1!",
      "nueva",
      "nueva",
      requestMock
    );

    assertEquals(ReglasDeContrasena.MENSAJE, resultado.getModel().get("errorContrasena"));
  }

  @Test
  public void debeEliminarLaCuentaCerrarLaSesionYVolverAlLoginConElAviso() {
    ModelAndView resultado = controladorPerfil.eliminarCuenta("Actual1!", requestMock);

    verify(servicioCuentaMock).eliminarCuenta(usuarioMock, "Actual1!");
    verify(sessionMock).invalidate();
    assertEquals("redirect:/login?cuentaEliminada", resultado.getViewName());
  }

  @Test
  public void noDebeEliminarNiCerrarLaSesionCuandoLaContrasenaEsIncorrecta() {
    doThrow(new ContrasenaIncorrecta())
      .when(servicioCuentaMock)
      .eliminarCuenta(usuarioMock, "Mal1234!");

    ModelAndView resultado = controladorPerfil.eliminarCuenta("Mal1234!", requestMock);

    assertEquals("perfil", resultado.getViewName());
    assertEquals(
      "La contraseña no es correcta: la cuenta no se eliminó",
      resultado.getModel().get("errorEliminar")
    );
    verify(sessionMock, never()).invalidate();
  }

  @Test
  public void noDebeEliminarNadaSinSesion() {
    when(requestMock.getSession(false)).thenReturn(null);

    ModelAndView resultado = controladorPerfil.eliminarCuenta("Actual1!", requestMock);

    assertEquals("redirect:/login", resultado.getViewName());
    verify(servicioCuentaMock, never()).eliminarCuenta(any(), any());
  }
}
