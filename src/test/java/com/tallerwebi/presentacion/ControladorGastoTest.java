package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
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
  public void debeMostrarLosGastosDelUsuario() {
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
  public void debeDevolverLaVistaSimulacionConLosGastosDelMesAnteriorQueTraeElServicio() {
    YearMonth mesActual = YearMonth.now();
    List<Gasto> gastos = List.of(gasto);
    when(gastoServicio.obtenerGastosDelMesAnterior(usuario, mesActual)).thenReturn(gastos);

    ModelAndView resultado = controladorGasto.mostrarSimulacion(request);

    assertEquals("simulacion", resultado.getViewName());
    assertEquals(gastos, resultado.getModel().get("gastos"));
    verify(gastoServicio).obtenerGastosDelMesAnterior(usuario, mesActual);
  }

  @Test
  public void debeTenerComoMesBaseElAnteriorComoMesSimuladoElProximoYPresupuestoCero() {
    YearMonth mesActual = YearMonth.now();
    when(gastoServicio.obtenerGastosDelMesAnterior(usuario, mesActual)).thenReturn(List.of());

    ModelAndView resultado = controladorGasto.mostrarSimulacion(request);

    assertEquals(nombreDelMes(mesActual.minusMonths(1)), resultado.getModel().get("mesBase"));
    assertEquals(nombreDelMes(mesActual.plusMonths(1)), resultado.getModel().get("mesSimulado"));
    assertEquals(0, resultado.getModel().get("presupuesto"));
  }

  @Test
  public void debeRedirigirALoginSinConsultarElServicioSiNoHayUsuarioEnLaSimulacion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorGasto.mostrarSimulacion(request);

    assertEquals("redirect:/login", resultado.getViewName());
    verifyNoInteractions(gastoServicio);
  }

  private String nombreDelMes(YearMonth mes) {
    String nombre = mes.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "AR"));
    return nombre.substring(0, 1).toUpperCase() + nombre.substring(1) + " " + mes.getYear();
  }

  @Test
  public void debeRegistrarUnGasto() {
    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request);

    assertEquals("redirect:/mostrar-gastos", resultado.getViewName());

    verify(gastoServicio).registrarGasto(gasto, usuario);
  }

  @Test
  public void debeMostrarElFormularioDeRegistroConLaFechaDeHoy() {
    ModelAndView resultado = controladorGasto.mostrarFormularioRegistrarGasto(request);

    assertEquals("registrar-gasto", resultado.getViewName());

    Gasto gastoDelFormulario = (Gasto) resultado.getModel().get("gasto");
    assertEquals(LocalDate.now(), gastoDelFormulario.getFecha());
  }

  @Test
  public void debeVolverAlFormularioConErrorSiElGastoEsInvalido() {
    doThrow(new GastoInvalidoExeption("El importe debe ser mayor a cero"))
      .when(gastoServicio)
      .registrarGasto(gasto, usuario);

    ModelAndView resultado = controladorGasto.registrarGasto(gasto, request);

    assertEquals("registrar-gasto", resultado.getViewName());
    assertEquals("El importe debe ser mayor a cero", resultado.getModel().get("error"));
    assertEquals(gasto, resultado.getModel().get("gasto"));
  }

  @Test
  public void debeSumarLosGastosEntreFechas() {
    LocalDate desde = LocalDate.of(2026, 9, 1);
    LocalDate hasta = LocalDate.of(2026, 9, 30);

    when(gastoServicio.sumarGastos(usuario, desde, hasta)).thenReturn(45000.0);

    ModelAndView resultado = controladorGasto.sumarGastos(request, desde, hasta);

    assertEquals("sumar-gastos", resultado.getViewName());

    assertEquals(45000.0, resultado.getModel().get("total"));

    verify(gastoServicio).sumarGastos(usuario, desde, hasta);
  }

  @Test
  public void debeRedirigirALoginSiNoHayUsuarioEnSesion() {
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
  public void debeRedirigirALoginSiNoExisteLaSesion() {
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
