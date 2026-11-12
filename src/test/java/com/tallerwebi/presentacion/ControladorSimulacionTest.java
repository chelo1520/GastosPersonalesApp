package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.simulacion.ServicioSimulacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

public class ControladorSimulacionTest {

  private GastoServicio gastoServicio;
  private ServicioPresupuesto servicioPresupuesto;
  private ServicioSimulacion servicioSimulacion;
  private ControladorSimulacion controlador;
  private Usuario usuario;
  private HttpServletRequest request;
  private HttpSession session;

  @BeforeEach
  public void init() {
    gastoServicio = mock(GastoServicio.class);
    servicioPresupuesto = mock(ServicioPresupuesto.class);
    servicioSimulacion = mock(ServicioSimulacion.class);
    controlador = new ControladorSimulacion(gastoServicio, servicioPresupuesto, servicioSimulacion);

    usuario = mock(Usuario.class);
    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("USUARIO")).thenReturn(usuario);
  }

  @Test
  public void debeUsarElPresupuestoDelMesSimuladoCuandoEstaDefinido() {
    YearMonth mesActual = YearMonth.now();
    when(servicioPresupuesto.presupuestoDelMes(usuario, mesActual.plusMonths(1)))
      .thenReturn(480000.0);
    when(servicioPresupuesto.presupuestoDelMes(usuario, mesActual)).thenReturn(400000.0);

    ModelAndView mav = controlador.mostrarSimulacion(null, null, request);

    assertEquals("simulacion", mav.getViewName());
    assertEquals(480000.0, mav.getModel().get("presupuesto"));
  }

  @Test
  public void debeUsarElPresupuestoDelMesActualCuandoElMesSimuladoNoTieneNingunoDefinido() {
    YearMonth mesActual = YearMonth.now();
    when(servicioPresupuesto.presupuestoDelMes(usuario, mesActual.plusMonths(1))).thenReturn(0.0);
    when(servicioPresupuesto.presupuestoDelMes(usuario, mesActual)).thenReturn(400000.0);

    ModelAndView mav = controlador.mostrarSimulacion(null, null, request);

    assertEquals(400000.0, mav.getModel().get("presupuesto"));
  }

  @Test
  public void debeRedirigirALoginSinConsultarNadaCuandoNoHayUsuarioEnSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    assertEquals(
      "redirect:/login",
      controlador.mostrarSimulacion(null, null, request).getViewName()
    );
    verifyNoInteractions(gastoServicio, servicioPresupuesto, servicioSimulacion);
  }

  @Test
  public void debeUsarLosMesesElegidosParaLosGastosYElPresupuesto() {
    YearMonth mesBase = YearMonth.now().minusMonths(2);
    YearMonth mesSimulado = YearMonth.now().plusMonths(2);
    when(servicioPresupuesto.presupuestoDelMes(usuario, mesSimulado)).thenReturn(650000.0);

    ModelAndView mav = controlador.mostrarSimulacion(
      mesBase.toString(),
      mesSimulado.toString(),
      request
    );

    verify(gastoServicio).obtenerGastosDelMes(usuario, mesBase);
    verify(servicioPresupuesto).presupuestoDelMes(usuario, mesSimulado);
    assertEquals(mesBase.toString(), mav.getModel().get("mesBase"));
    assertEquals(mesSimulado.toString(), mav.getModel().get("mesSimulado"));
    assertEquals(650000.0, mav.getModel().get("presupuesto"));
  }

  @Test
  public void debeRechazarUnMesConFormatoInvalido() {
    assertThrows(
      ResponseStatusException.class,
      () -> controlador.mostrarSimulacion("Junio", null, request)
    );
  }
}
