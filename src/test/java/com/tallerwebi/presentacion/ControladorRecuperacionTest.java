package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ReglasDeContrasena;
import com.tallerwebi.dominio.excepcion.TokenRecuperacionInvalido;
import com.tallerwebi.dominio.recuperacion.ServicioRecuperacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorRecuperacionTest {

  private ServicioRecuperacion servicioRecuperacion;
  private ControladorRecuperacion controlador;

  @BeforeEach
  public void init() {
    servicioRecuperacion = mock(ServicioRecuperacion.class);
    controlador = new ControladorRecuperacion(servicioRecuperacion);
  }

  @Test
  public void debePedirElMailYMostrarElAvisoDeEnviado() {
    ModelAndView resultado = controlador.pedirMail("lour@gastitos.com");

    verify(servicioRecuperacion).solicitar("lour@gastitos.com");
    assertEquals("olvide-contrasena", resultado.getViewName());
    assertEquals(true, resultado.getModel().get("enviado"));
  }

  @Test
  public void debeMostrarElFormularioConElTokenCuandoElLinkEsValido() {
    when(servicioRecuperacion.esValido("abc")).thenReturn(true);

    ModelAndView resultado = controlador.irARestablecer("abc");

    assertEquals("restablecer-contrasena", resultado.getViewName());
    assertEquals("abc", resultado.getModel().get("token"));
    assertNull(resultado.getModel().get("linkInvalido"));
  }

  @Test
  public void debeAvisarQueElLinkNoSirveCuandoVencioOYaSeUso() {
    when(servicioRecuperacion.esValido("abc")).thenReturn(false);

    ModelAndView resultado = controlador.irARestablecer("abc");

    assertEquals(true, resultado.getModel().get("linkInvalido"));
    assertNull(resultado.getModel().get("token"));
  }

  @Test
  public void debeCambiarLaContrasenaYVolverAlLoginConElAviso() {
    ModelAndView resultado = controlador.restablecer("abc", "Nueva123!", "Nueva123!");

    verify(servicioRecuperacion).restablecer("abc", "Nueva123!");
    assertEquals("redirect:/login?restablecida", resultado.getViewName());
  }

  @Test
  public void noDebeCambiarlaCuandoLasContrasenasNoCoinciden() {
    ModelAndView resultado = controlador.restablecer("abc", "Nueva123!", "Otra1234!");

    verify(servicioRecuperacion, never()).restablecer(anyString(), anyString());
    assertEquals("Las contraseñas no coinciden", resultado.getModel().get("error"));
    assertEquals("abc", resultado.getModel().get("token"));
  }

  @Test
  public void noDebeCambiarlaCuandoLaContrasenaEsCorta() {
    ModelAndView resultado = controlador.restablecer("abc", "123", "123");

    verify(servicioRecuperacion, never()).restablecer(any(), any());
    assertEquals(ReglasDeContrasena.MENSAJE, resultado.getModel().get("error"));
  }

  @Test
  public void debeAvisarQueElLinkNoSirveCuandoVencioMientrasCompletabaElFormulario() {
    doThrow(TokenRecuperacionInvalido.class)
      .when(servicioRecuperacion)
      .restablecer("abc", "Nueva123!");

    ModelAndView resultado = controlador.restablecer("abc", "Nueva123!", "Nueva123!");

    assertEquals("restablecer-contrasena", resultado.getViewName());
    assertEquals(true, resultado.getModel().get("linkInvalido"));
  }
}
