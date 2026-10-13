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
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
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
    categoriaRepositorioMock = mock(CategoriaRepositorio.class);
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
      new ControladorPresupuesto(servicioPresupuestoMock, categoriaRepositorioMock, HOY);
  }

  @Test
  public void debeRedirigirAlLoginSinConsultarElServicioCuandoNoHayUsuarioEnSesion() {
    when(sessionMock.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView mav = controlador.mostrarPresupuestos(null, requestMock);

    assertThat(mav.getViewName(), equalTo("redirect:/login"));
    verify(servicioPresupuestoMock, never()).obtenerDelMes(any(), any());
  }

  @Test
  public void debeMostrarElMesDelRelojCuandoNoSePideUnMes() {
    ModelAndView mav = controlador.mostrarPresupuestos(null, requestMock);

    assertThat(mav.getViewName(), equalTo("presupuestos"));
    assertThat(mav.getModel().get("mes"), equalTo("2026-10"));
    assertThat(mav.getModel().get("mesActual"), equalTo("Octubre 2026"));
    verify(servicioPresupuestoMock).obtenerDelMes(usuario, OCTUBRE);
  }

  @Test
  public void debeMostrarElMesPedidoConSusVecinosCuandoSePideUnMesValido() {
    ModelAndView mav = controlador.mostrarPresupuestos("2026-09", requestMock);

    assertThat(mav.getModel().get("mesAnterior"), equalTo("2026-08"));
    assertThat(mav.getModel().get("mesSiguiente"), equalTo("2026-10"));
    verify(servicioPresupuestoMock).obtenerDelMes(usuario, YearMonth.of(2026, 9));
  }

  @Test
  public void debeMostrarElMesActualCuandoElMesPedidoEsInvalido() {
    ModelAndView mav = controlador.mostrarPresupuestos("octubre", requestMock);

    assertThat(mav.getModel().get("mes"), equalTo("2026-10"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void debeListarTodasLasCategoriasConSuMontoOSinPresupuestoYElTotalDelMes() {
    when(servicioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE))
      .thenReturn(List.of(new Presupuesto(usuario, alimentacion, OCTUBRE, 130000.0)));

    ModelAndView mav = controlador.mostrarPresupuestos(null, requestMock);

    List<FilaPresupuesto> filas = (List<FilaPresupuesto>) mav.getModel().get("filas");
    assertThat(filas, hasSize(2));
    assertThat(filas.get(0).getCategoria(), equalTo("Alimentación"));
    assertThat(filas.get(0).getMonto(), equalTo(130000.0));
    assertThat(filas.get(1).getCategoria(), equalTo("Transporte"));
    assertThat(filas.get(1).getMonto(), nullValue());
    assertThat(mav.getModel().get("totalPresupuestado"), equalTo(130000.0));
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

  private Categoria categoria(Long id, String nombre) {
    Categoria categoria = new Categoria();
    categoria.setId(id);
    categoria.setNombre(nombre);
    return categoria;
  }
}
