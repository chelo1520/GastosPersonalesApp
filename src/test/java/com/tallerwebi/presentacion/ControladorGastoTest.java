package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorGastoTest {

  private GastoServicio gastoServicio;
  private ControladorGasto controladorGasto;

  private Usuario usuario;
  private Gasto gasto;
  private HttpServletRequest request;
  private HttpSession session;

  @BeforeEach
  public void init() {
    gastoServicio = mock(GastoServicio.class);

    controladorGasto = new ControladorGasto(gastoServicio);

    usuario = mock(Usuario.class);
    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("USUARIO")).thenReturn(usuario);

    gasto = new Gasto(15000.0, LocalDate.of(2026, 9, 26), "Supermercado");
  }

  @Test
  public void debeMostrarLosGastosOrdenadosConTotal20000CuandoElUsuarioTieneGastosDe15000Y5000() {
    Gasto gastoAnterior = new Gasto(5000.0, LocalDate.of(2026, 9, 20), "Farmacia");
    List<Gasto> gastos = List.of(gastoAnterior, gasto);

    when(gastoServicio.obtenerGastos(usuario)).thenReturn(gastos);

    ModelAndView resultado = controladorGasto.mostrarGastos(request);

    assertEquals("mostrar-gastos", resultado.getViewName());

    assertEquals(List.of(gasto, gastoAnterior), resultado.getModel().get("gastos"));
    assertEquals(20000.0, resultado.getModel().get("total"));

    verify(gastoServicio).obtenerGastos(usuario);
  }

  @Test
  public void debeRegistrarElGastoYRedirigirAMisGastosCuandoElGastoEsValido() {
    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());

    verify(gastoServicio).registrarGasto(gasto, usuario);
  }

  @Test
  public void debeMostrarElFormularioConLaFechaDeHoyCuandoSeEntraARegistrarGasto() {
    ModelAndView resultado = controladorGasto.mostrarFormularioRegistrarGasto(request);

    assertEquals("registrar-gasto", resultado.getViewName());

    Gasto gastoDelFormulario = (Gasto) resultado.getModel().get("gasto");
    assertEquals(LocalDate.now(), gastoDelFormulario.getFecha());
  }

  @Test
  public void debeLimitarElCalendarioAlMesActualCuandoSeEntraARegistrarGasto() {
    when(gastoServicio.obtenerFechaMinimaPermitida()).thenReturn(LocalDate.of(2026, 10, 1));
    when(gastoServicio.obtenerFechaMaximaPermitida()).thenReturn(LocalDate.of(2026, 10, 31));

    ModelAndView resultado = controladorGasto.mostrarFormularioRegistrarGasto(request);

    assertEquals(LocalDate.of(2026, 10, 1), resultado.getModel().get("fechaMinima"));
    assertEquals(LocalDate.of(2026, 10, 31), resultado.getModel().get("fechaMaxima"));
  }

  @Test
  public void debeVolverAlFormularioConElMensajeDeErrorCuandoElServicioRechazaElGasto() {
    doThrow(new GastoInvalidoExeption("El importe debe ser mayor a cero"))
      .when(gastoServicio)
      .registrarGasto(gasto, usuario);

    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals("El importe debe ser mayor a cero", resultado.getModel().get("error"));
    assertEquals(gasto, resultado.getModel().get("gasto"));
  }

  @Test
  public void debeMostrarElFormularioPrecargadoConElGastoCuandoSeEntraAModificarGasto() {
    when(gastoServicio.obtenerGasto(10L, usuario)).thenReturn(gasto);

    ModelAndView resultado = controladorGasto.mostrarFormularioModificarGasto(10L, request);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals(gasto, resultado.getModel().get("gasto"));
    assertEquals("Modificar gasto", resultado.getModel().get("titulo"));
    assertEquals("/modificar-gasto/10", resultado.getModel().get("accion"));
    assertEquals("/mostrar-gastos", resultado.getModel().get("urlCancelar"));
  }

  @Test
  public void debeRedirigirAMisGastosCuandoSeQuiereModificarUnGastoInexistenteODeOtroUsuario() {
    when(gastoServicio.obtenerGasto(99L, usuario)).thenThrow(new GastoNoEncontrado());

    ModelAndView resultado = controladorGasto.mostrarFormularioModificarGasto(99L, request);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());
  }

  @Test
  public void debeModificarElGastoYRedirigirAMisGastosCuandoLosDatosSonValidos() {
    ModelAndView resultado = controladorGasto.modificarGasto(10L, gasto, request);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());

    verify(gastoServicio).modificarGasto(10L, gasto, usuario);
  }

  @Test
  public void debeVolverAlFormularioDeModificarConElErrorCuandoElServicioRechazaLosDatos() {
    doThrow(new GastoInvalidoExeption("El importe debe ser mayor a cero"))
      .when(gastoServicio)
      .modificarGasto(10L, gasto, usuario);

    ModelAndView resultado = controladorGasto.modificarGasto(10L, gasto, request);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals("El importe debe ser mayor a cero", resultado.getModel().get("error"));
    assertEquals(gasto, resultado.getModel().get("gasto"));
    assertEquals("/modificar-gasto/10", resultado.getModel().get("accion"));
  }

  @Test
  public void debeRedirigirALoginSinModificarCuandoNoHayUsuarioEnSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    assertEquals(
      "redirect:/login",
      controladorGasto.mostrarFormularioModificarGasto(10L, request).getViewName()
    );
    assertEquals(
      "redirect:/login",
      controladorGasto.modificarGasto(10L, gasto, request).getViewName()
    );

    verifyNoInteractions(gastoServicio);
  }

  @Test
  public void debeMostrarTotal45000CuandoLosGastosEntreLasFechasSuman45000() {
    LocalDate desde = LocalDate.of(2026, 9, 1);
    LocalDate hasta = LocalDate.of(2026, 9, 30);

    when(gastoServicio.sumarGastos(usuario, desde, hasta)).thenReturn(45000.0);

    ModelAndView resultado = controladorGasto.sumarGastos(request, desde, hasta);

    assertEquals("sumar-gastos", resultado.getViewName());

    assertEquals(45000.0, resultado.getModel().get("total"));

    verify(gastoServicio).sumarGastos(usuario, desde, hasta);
  }

  @Test
  public void debeRedirigirALoginSinConsultarElServicioCuandoNoHayUsuarioEnSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    assertEquals("redirect:/login", controladorGasto.mostrarGastos(request).getViewName());
    assertEquals(
      "redirect:/login",
      controladorGasto.mostrarFormularioRegistrarGasto(request).getViewName()
    );
    assertEquals("redirect:/login", controladorGasto.registrarGasto(gasto, request).getViewName());
    assertEquals(
      "redirect:/login",
      controladorGasto
        .sumarGastos(request, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        .getViewName()
    );

    verifyNoInteractions(gastoServicio);
  }

  @Test
  public void debeRedirigirALoginSinConsultarElServicioCuandoNoExisteLaSesion() {
    when(request.getSession(false)).thenReturn(null);

    assertEquals("redirect:/login", controladorGasto.mostrarGastos(request).getViewName());
    assertEquals(
      "redirect:/login",
      controladorGasto.mostrarFormularioRegistrarGasto(request).getViewName()
    );
    assertEquals("redirect:/login", controladorGasto.registrarGasto(gasto, request).getViewName());
    assertEquals(
      "redirect:/login",
      controladorGasto
        .sumarGastos(request, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        .getViewName()
    );

    verifyNoInteractions(gastoServicio);
  }
}
