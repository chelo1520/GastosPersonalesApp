package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorResumenTest {

  // "Hoy" fijo: 8 de octubre de 2026
  private static final Clock HOY = Clock.fixed(
    Instant.parse("2026-10-08T12:00:00Z"),
    ZoneId.of("America/Argentina/Buenos_Aires")
  );

  private ServicioResumen servicioResumen;
  private ControladorResumen controladorResumen;
  private Usuario usuario;
  private HttpServletRequest request;
  private HttpSession session;
  private ResumenMensual resumen;

  @BeforeEach
  public void init() {
    servicioResumen = mock(ServicioResumen.class);
    controladorResumen = new ControladorResumen(servicioResumen, HOY);

    usuario = mock(Usuario.class);
    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("USUARIO")).thenReturn(usuario);

    List<ResumenCategoria> categorias = List.of(
      new ResumenCategoria("Sin categoría", 45000.0, 100.0, -45000.0, 100.0)
    );
    resumen = new ResumenMensual(45000.0, 0.0, -45000.0, 100.0, categorias);
  }

  @Test
  public void debeMostrarElResumenDeOctubreCuandoNoSeIndicaMesYHoyEsOctubre() {
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(resumen);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals("resumen", resultado.getViewName());
    assertEquals("Octubre 2026", resultado.getModel().get("mesActual"));
    assertEquals("2026-09", resultado.getModel().get("mesAnterior"));
    assertEquals("2026-11", resultado.getModel().get("mesSiguiente"));
    verify(servicioResumen).obtenerResumen(usuario, YearMonth.of(2026, 10));
  }

  @Test
  public void debeMostrarElResumenDeSeptiembreCuandoSePideElMes2026Guion09() {
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 9))).thenReturn(resumen);

    ModelAndView resultado = controladorResumen.mostrarResumen("2026-09", request);

    assertEquals("Septiembre 2026", resultado.getModel().get("mesActual"));
    assertEquals("2026-08", resultado.getModel().get("mesAnterior"));
    assertEquals("2026-10", resultado.getModel().get("mesSiguiente"));
  }

  @Test
  public void debePasarALaVistaLosTotalesYCategoriasQueCalculaElServicio() {
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(resumen);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals(45000.0, resultado.getModel().get("gastado"));
    assertEquals(0.0, resultado.getModel().get("presupuesto"));
    assertEquals(-45000.0, resultado.getModel().get("disponible"));
    assertEquals(100.0, resultado.getModel().get("porcentajeUsado"));
    assertEquals(resumen.getCategorias(), resultado.getModel().get("categorias"));
  }

  @Test
  public void debeMostrarElMesActualCuandoElMesPedidoNoTieneFormatoValido() {
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(resumen);

    ModelAndView resultado = controladorResumen.mostrarResumen("septiembre", request);

    assertEquals("Octubre 2026", resultado.getModel().get("mesActual"));
  }

  @Test
  public void debeRedirigirALoginSinConsultarElServicioCuandoNoHayUsuarioEnSesion() {
    when(session.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals("redirect:/login", resultado.getViewName());
    verifyNoInteractions(servicioResumen);
  }

  @Test
  public void debeRedirigirALoginSinConsultarElServicioCuandoNoExisteLaSesion() {
    when(request.getSession(false)).thenReturn(null);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals("redirect:/login", resultado.getViewName());
    verifyNoInteractions(servicioResumen);
  }
}
