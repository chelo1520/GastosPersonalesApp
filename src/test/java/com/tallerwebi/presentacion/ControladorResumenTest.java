package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenDeMes;
import com.tallerwebi.dominio.resumen.ResumenHormiga;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
  private ServicioPresupuesto servicioPresupuesto;
  private GastoServicio gastoServicio;
  private ControladorResumen controladorResumen;
  private Usuario usuario;
  private HttpServletRequest request;
  private HttpSession session;
  private ResumenMensual resumen;

  @BeforeEach
  public void init() {
    servicioResumen = mock(ServicioResumen.class);
    servicioPresupuesto = mock(ServicioPresupuesto.class);
    gastoServicio = mock(GastoServicio.class);
    controladorResumen =
      new ControladorResumen(servicioResumen, servicioPresupuesto, gastoServicio, HOY);

    usuario = mock(Usuario.class);
    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("USUARIO")).thenReturn(usuario);

    List<ResumenCategoria> categorias = List.of(
      new ResumenCategoria("Sin categoría", 45000.0, 100.0)
    );
    ResumenHormiga hormiga = new ResumenHormiga(0.0, 0.0, List.of());
    resumen = new ResumenMensual(45000.0, 0.0, -45000.0, 100.0, categorias, hormiga, List.of());
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
  public void debePasarALaVistaLosTotalesCategoriasYGastosHormigaQueCalculaElServicio() {
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(resumen);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals(45000.0, resultado.getModel().get("gastado"));
    assertEquals(0.0, resultado.getModel().get("presupuesto"));
    assertEquals(-45000.0, resultado.getModel().get("disponible"));
    assertEquals(100.0, resultado.getModel().get("porcentajeUsado"));
    assertEquals(resumen.getCategorias(), resultado.getModel().get("categorias"));
    assertEquals(resumen.getHormiga(), resultado.getModel().get("hormiga"));
    assertEquals(resumen.getAlertas(), resultado.getModel().get("alertas"));
  }

  @Test
  public void debePasarALaVistaElPresupuestoMensualDelPerfilYElPorcentajeGastadoDeEl() {
    when(servicioPresupuesto.presupuestoDelMes(usuario, YearMonth.of(2026, 10)))
      .thenReturn(180000.0);
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(resumen);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals(180000.0, resultado.getModel().get("presupuestoMensual"));
    assertEquals(25.0, resultado.getModel().get("porcentajeMensualUsado"));
    assertEquals(135000.0, resultado.getModel().get("disponibleMensual"));
    assertEquals(0.0, resultado.getModel().get("asignado"));
    assertEquals(180000.0, resultado.getModel().get("sinAsignar"));
  }

  @Test
  public void debePasarPorcentajeCeroCuandoElPerfilNoDefinioPresupuestoMensual() {
    when(servicioPresupuesto.presupuestoDelMes(usuario, YearMonth.of(2026, 10))).thenReturn(0.0);
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(resumen);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals(0.0, resultado.getModel().get("presupuestoMensual"));
    assertEquals(0.0, resultado.getModel().get("porcentajeMensualUsado"));
  }

  @Test
  public void debePasarALaVistaElHistorialDe6MesesHastaElMesMostradoConSuNombreCorto() {
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 9))).thenReturn(resumen);
    ResumenDeMes agosto = new ResumenDeMes(
      YearMonth.of(2026, 8),
      500000.0,
      30000.0,
      550000.0,
      false
    );
    ResumenDeMes septiembre = new ResumenDeMes(
      YearMonth.of(2026, 9),
      45000.0,
      0.0,
      550000.0,
      false
    );
    when(servicioResumen.obtenerHistorial(usuario, YearMonth.of(2026, 9), 6))
      .thenReturn(List.of(agosto, septiembre));

    ModelAndView resultado = controladorResumen.mostrarResumen("2026-09", request);

    @SuppressWarnings("unchecked")
    List<MesDelHistorial> historial = (List<MesDelHistorial>) resultado.getModel().get("historial");
    assertEquals(2, historial.size());
    assertEquals("Ago 2026", historial.get(0).getEtiqueta());
    assertEquals(agosto, historial.get(0).getResumen());
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

  @Test
  public void debeUsarElPresupuestoTotalDelMesMostradoYMarcarElExcesoDeGasto() {
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(resumen);
    when(servicioPresupuesto.presupuestoDelMes(usuario, YearMonth.of(2026, 10)))
      .thenReturn(40000.0);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals(40000.0, resultado.getModel().get("presupuestoMensual"));
    assertEquals(-5000.0, resultado.getModel().get("disponibleMensual"));
    assertEquals(true, resultado.getModel().get("excedido"));
  }

  @Test
  public void debeArmarLosUltimosMovimientosConLaFechaRelativaAHoy() {
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(resumen);
    Categoria delivery = new Categoria();
    delivery.setNombre("Delivery");
    Gasto deHoy = new Gasto(14350.0, LocalDate.of(2026, 10, 8), "Rappi");
    deHoy.setCategoria(delivery);
    Gasto deAyer = new Gasto(6900.0, LocalDate.of(2026, 10, 7), "Café");
    Gasto viejo = new Gasto(52000.0, LocalDate.of(2026, 9, 22), "Nafta");
    when(gastoServicio.obtenerUltimos(usuario, ControladorResumen.MOVIMIENTOS_RECIENTES))
      .thenReturn(List.of(deHoy, deAyer, viejo));

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    @SuppressWarnings("unchecked")
    List<MovimientoReciente> movimientos = (List<MovimientoReciente>) resultado
      .getModel()
      .get("movimientos");
    assertEquals(3, movimientos.size());
    assertEquals("Hoy", movimientos.get(0).getCuando());
    assertEquals("Delivery", movimientos.get(0).getCategoria());
    assertEquals("D", movimientos.get(0).getInicial());
    assertEquals("#c2510e", movimientos.get(0).getColor());
    assertEquals("Ayer", movimientos.get(1).getCuando());
    assertEquals("Sin categoría", movimientos.get(1).getCategoria());
    assertEquals("22 sept", movimientos.get(2).getCuando());
  }

  @Test
  public void elPorcentajeHormigaSobreLoGastadoSaleDelResumen() {
    ResumenHormiga hormiga = new ResumenHormiga(9000.0, 3.0, List.of(), 6);
    ResumenMensual conHormiga = new ResumenMensual(
      45000.0,
      300000.0,
      255000.0,
      15.0,
      List.of(),
      hormiga,
      List.of()
    );
    when(servicioResumen.obtenerResumen(usuario, YearMonth.of(2026, 10))).thenReturn(conHormiga);

    ModelAndView resultado = controladorResumen.mostrarResumen(null, request);

    assertEquals(20.0, (Double) resultado.getModel().get("hormigaPorcentajeGastado"), 0.01);
  }
}
