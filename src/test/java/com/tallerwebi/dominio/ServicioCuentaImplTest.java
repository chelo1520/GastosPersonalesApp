package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.ContrasenaIncorrecta;
import com.tallerwebi.dominio.excepcion.ContrasenaInvalida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioCuentaImplTest {

  private RepositorioUsuario repositorioUsuario;
  private ServicioCuentaImpl servicio;
  private Usuario enSesion;
  private Usuario guardado;

  @BeforeEach
  public void init() {
    repositorioUsuario = mock(RepositorioUsuario.class);
    servicio = new ServicioCuentaImpl(repositorioUsuario);

    enSesion = new Usuario();
    enSesion.setEmail("lour@gastitos.com");
    guardado = new Usuario();
    guardado.setEmail("lour@gastitos.com");
    guardado.setPassword("Actual1!");
    when(repositorioUsuario.buscar("lour@gastitos.com")).thenReturn(guardado);
  }

  @Test
  public void debeCambiarLaContrasenaCuandoLaActualEsCorrectaYLaNuevaCumpleLasReglas() {
    Usuario resultado = servicio.cambiarContrasena(enSesion, "Actual1!", "Nueva123!");

    assertSame(guardado, resultado);
    assertEquals("Nueva123!", guardado.getPassword());
    verify(repositorioUsuario).modificar(guardado);
  }

  @Test
  public void noDebeCambiarlaCuandoLaContrasenaActualEsIncorrecta() {
    assertThrows(
      ContrasenaIncorrecta.class,
      () -> servicio.cambiarContrasena(enSesion, "Otra123!", "Nueva123!")
    );

    assertEquals("Actual1!", guardado.getPassword());
    verify(repositorioUsuario, never()).modificar(any());
  }

  @Test
  public void noDebeCambiarlaCuandoLaNuevaNoCumpleLasReglas() {
    assertThrows(
      ContrasenaInvalida.class,
      () -> servicio.cambiarContrasena(enSesion, "Actual1!", "nueva")
    );

    assertEquals("Actual1!", guardado.getPassword());
    verify(repositorioUsuario, never()).modificar(any());
  }

  @Test
  public void debeEliminarLaCuentaCuandoLaContrasenaEsCorrecta() {
    servicio.eliminarCuenta(enSesion, "Actual1!");

    verify(repositorioUsuario).eliminar(guardado);
  }

  @Test
  public void noDebeEliminarLaCuentaCuandoLaContrasenaEsIncorrectaOFalta() {
    assertThrows(ContrasenaIncorrecta.class, () -> servicio.eliminarCuenta(enSesion, "Otra123!"));
    assertThrows(ContrasenaIncorrecta.class, () -> servicio.eliminarCuenta(enSesion, null));

    verify(repositorioUsuario, never()).eliminar(any());
  }

  @Test
  public void noDebeEliminarNadaCuandoElUsuarioYaNoExiste() {
    when(repositorioUsuario.buscar("lour@gastitos.com")).thenReturn(null);

    assertThrows(ContrasenaIncorrecta.class, () -> servicio.eliminarCuenta(enSesion, "Actual1!"));

    verify(repositorioUsuario, never()).eliminar(any());
  }
}
