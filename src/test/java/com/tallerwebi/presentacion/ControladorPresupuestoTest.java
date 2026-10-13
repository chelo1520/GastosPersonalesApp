package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.resumen.EstadoPresupuesto;
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

public class ControladorPresupuestoTest {

  // "Hoy" fijo: 13 de octubre de 2026
  private static final Clock HOY = Clock.fixed(
    Instant.parse("2026-10-13T12:00:00Z"),
    ZoneId.of("America/Argentina/Buenos_Aires")
  );
  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

  private ServicioPresupuesto servicioPresupuestoMock;
  private ServicioResumen servicioResumenMock;
  private CategoriaRepositorio categoriaRepositorioMock;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private Usuario usuario;
  private Categoria alimentacion;
  private Categoria transporte;
  private ControladorPresupuesto controlador;

  @BeforeEach
  public void init() {
    servicioPresupuestoMock = mock(ServicioPresupuesto.class);
    servicioResumenMock = mock(ServicioResumen.class);
    categoriaRepositorioMock = mock(CategoriaRepositorio.class);
    // Sin gastos ni presupuestos salvo que el test diga lo contrario
    when(servicioResumenMock.obtenerResumen(any(), any())).thenReturn(resumenVacio());
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    usuario = new Usuario();
    usuario.setEmail("diego@unlam.edu.ar");

    alimentacion = categoria(1L, "Alimentación");
    transporte = categoria(2L, "Transporte");
    when(categoriaRepositorioMock.obtenerTodas()).thenReturn(List.of(alimentacion, transporte));
    when(categoriaRepositorioMock.buscarPorId(1L)).thenReturn(alimentacion);

    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("USUARIO")).thenReturn(usuario);

    controlador =
      new ControladorPresupuesto(
        servicioPresupuestoMock,
        servicioResumenMock,
        categoriaRepositorioMock,
        HOY
      );
  }

  @Test
  public void debeRedirigirAlLoginSinConsultarElServicioCuandoNoHayUsuarioEnSesion() {
    when(sessionMock.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView mav = controlador.mostrarPresupuestos(null, requestMock);

    assertThat(mav.getViewName(), equalTo("redirect:/login"));
    verify(servicioResumenMock, never()).obtenerResumen(any(), any());
  }

  @Test
  public void debeMostrarElMesDelRelojCuandoNoSePideUnMes() {
    ModelAndView mav = controlador.mostrarPresupuestos(null, requestMock);

    assertThat(mav.getViewName(), equalTo("presupuestos"));
    assertThat(mav.getModel().get("mes"), equalTo("2026-10"));
    assertThat(mav.getModel().get("mesActual"), equalTo("Octubre 2026"));
    verify(servicioResumenMock).obtenerResumen(usuario, OCTUBRE);
  }

  @Test
  public void debeMostrarElMesPedidoConSusVecinosCuandoSePideUnMesValido() {
    ModelAndView mav = controlador.mostrarPresupuestos("2026-09", requestMock);

    assertThat(mav.getModel().get("mesAnterior"), equalTo("2026-08"));
    assertThat(mav.getModel().get("mesSiguiente"), equalTo("2026-10"));
    verify(servicioResumenMock).obtenerResumen(usuario, YearMonth.of(2026, 9));
  }

  @Test
  public void debeMostrarElMesActualCuandoElMesPedidoEsInvalido() {
    ModelAndView mav = controlador.mostrarPresupuestos("octubre", requestMock);

    assertThat(mav.getModel().get("mes"), equalTo("2026-10"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void debeListarTodasLasCategoriasConSuEstadoYElTotalDelMesCuandoSoloUnaTienePresupuesto() {
    ResumenCategoria conPresupuesto = new ResumenCategoria(
      "Alimentación",
      118400.0,
      100.0,
      130000.0
    );
    when(servicioResumenMock.obtenerResumen(usuario, OCTUBRE))
      .thenReturn(new ResumenMensual(118400.0, 130000.0, 11600.0, 91.08, List.of(conPresupuesto)));

    ModelAndView mav = controlador.mostrarPresupuestos(null, requestMock);

    List<ResumenCategoria> filas = (List<ResumenCategoria>) mav.getModel().get("filas");
    assertThat(filas, hasSize(2));
    assertThat(filas.get(0).getNombre(), equalTo("Alimentación"));
    assertThat(filas.get(0).getPresupuesto(), equalTo(130000.0));
    assertThat(filas.get(0).getGastado(), equalTo(118400.0));
    assertThat(filas.get(0).getDisponible(), equalTo(11600.0));
    assertThat(filas.get(0).getEstado(), equalTo(EstadoPresupuesto.AMARILLO));
    assertThat(filas.get(1).getNombre(), equalTo("Transporte"));
    assertThat(filas.get(1).getPresupuesto(), nullValue());
    assertThat(filas.get(1).getGastado(), equalTo(0.0));
    assertThat(filas.get(1).getEstado(), equalTo(EstadoPresupuesto.SIN_PRESUPUESTO));
    assertThat(mav.getModel().get("totalPresupuestado"), equalTo(130000.0));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void debeAgregarAlFinalLosGastosSinCategoriaCuandoElResumenLosTrae() {
    ResumenCategoria sinCategoria = new ResumenCategoria("Sin categoría", 5000.0, 100.0);
    when(servicioResumenMock.obtenerResumen(usuario, OCTUBRE))
      .thenReturn(new ResumenMensual(5000.0, 0.0, -5000.0, 100.0, List.of(sinCategoria)));

    ModelAndView mav = controlador.mostrarPresupuestos(null, requestMock);

    List<ResumenCategoria> filas = (List<ResumenCategoria>) mav.getModel().get("filas");
    assertThat(filas, hasSize(3));
    assertThat(filas.get(2).getNombre(), equalTo("Sin categoría"));
    assertThat(filas.get(2).getGastado(), equalTo(5000.0));
  }

  @Test
  public void debeDefinirElPresupuestoYRedirigirAlMesCuandoLosDatosSonValidos() {
    DatosPresupuesto datos = new DatosPresupuesto(1L, 130000.0, "2026-10");

    ModelAndView mav = controlador.definirPresupuesto(datos, requestMock);

    verify(servicioPresupuestoMock).definir(usuario, alimentacion, OCTUBRE, 130000.0);
    assertThat(mav.getViewName(), equalTo("redirect:/presupuestos?mes=2026-10"));
  }

  @Test
  public void debeVolverALaVistaConElErrorCuandoElServicioRechazaElPresupuesto() {
    DatosPresupuesto datos = new DatosPresupuesto(1L, 0.0, "2026-10");
    doThrow(new PresupuestoInvalido("El monto debe ser mayor a cero"))
      .when(servicioPresupuestoMock)
      .definir(eq(usuario), eq(alimentacion), eq(OCTUBRE), any());

    ModelAndView mav = controlador.definirPresupuesto(datos, requestMock);

    assertThat(mav.getViewName(), equalTo("presupuestos"));
    assertThat(mav.getModel().get("error"), equalTo("El monto debe ser mayor a cero"));
    assertThat(mav.getModel().get("mes"), equalTo("2026-10"));
  }

  @Test
  public void debeRedirigirAlLoginSinDefinirNadaCuandoSeEnviaElFormularioSinSesion() {
    when(requestMock.getSession(false)).thenReturn(null);

    ModelAndView mav = controlador.definirPresupuesto(
      new DatosPresupuesto(1L, 130000.0, "2026-10"),
      requestMock
    );

    assertThat(mav.getViewName(), equalTo("redirect:/login"));
    verify(servicioPresupuestoMock, never()).definir(any(), any(), any(), any());
  }

  private ResumenMensual resumenVacio() {
    return new ResumenMensual(0.0, 0.0, 0.0, 0.0, List.of());
  }

  private Categoria categoria(Long id, String nombre) {
    Categoria categoria = new Categoria();
    categoria.setId(id);
    categoria.setNombre(nombre);
    return categoria;
  }
}
