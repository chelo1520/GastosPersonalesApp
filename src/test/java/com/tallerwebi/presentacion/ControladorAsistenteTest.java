package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.ServicioGastoReal;
import com.tallerwebi.dominio.asistente.Veredicto;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.consejo.ServicioConsejos;
import com.tallerwebi.dominio.excepcion.ConsejoNoEncontrado;
import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

public class ControladorAsistenteTest {

  private ServicioGastoReal servicioMock;
  private ServicioConsejos servicioConsejosMock;
  private ServicioPresupuesto servicioPresupuestoMock;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private Usuario usuario;
  private ControladorAsistente controlador;

  @BeforeEach
  public void init() {
    servicioMock = mock(ServicioGastoReal.class);
    servicioConsejosMock = mock(ServicioConsejos.class);
    servicioPresupuestoMock = mock(ServicioPresupuesto.class);
    when(servicioPresupuestoMock.obtenerDelMes(any(), any())).thenReturn(List.of());
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    usuario = new Usuario();
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("USUARIO")).thenReturn(usuario);
    when(servicioMock.calcular(any(), anyInt())).thenReturn(sinDatos());
    controlador =
      new ControladorAsistente(servicioMock, servicioConsejosMock, servicioPresupuestoMock);
  }

  @Test
  public void debeRedirigirAlLoginSinCalcularNadaCuandoNoHayUsuarioEnSesion() {
    when(sessionMock.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView mav = controlador.mostrarAsistente(6, null, requestMock);

    assertThat(mav.getViewName(), equalTo("redirect:/login"));
    verify(servicioMock, never()).calcular(any(), anyInt());
  }

  @Test
  public void debeUsarSeisMesesCuandoNoSePideUnPeriodo() {
    ModelAndView mav = controlador.mostrarAsistente(null, null, requestMock);

    assertThat(mav.getViewName(), equalTo("asistente"));
    assertThat(mav.getModel().get("periodo"), equalTo(6));
    verify(servicioMock).calcular(usuario, 6);
  }

  @Test
  public void debeUsarElPeriodoPedidoCuandoEsUnoDeLosPermitidos() {
    controlador.mostrarAsistente(12, null, requestMock);

    verify(servicioMock).calcular(usuario, 12);
  }

  @Test
  public void debeCaerEnSeisMesesCuandoElPeriodoPedidoNoEstaPermitido() {
    controlador.mostrarAsistente(7, null, requestMock);

    verify(servicioMock).calcular(usuario, 6);
  }

  @Test
  public void debeAvisarSinRomperCuandoLosIndicadoresNoEstanDisponibles() {
    when(servicioMock.calcular(any(), anyInt())).thenThrow(new IndicadoresNoDisponibles("sin red"));

    ModelAndView mav = controlador.mostrarAsistente(6, null, requestMock);

    assertThat(mav.getViewName(), equalTo("asistente"));
    assertThat(mav.getModel().get("indicadoresNoDisponibles"), equalTo(true));
    assertThat(mav.getModel().get("gastoReal"), nullValue());
  }

  @Test
  public void debeDejarElGastoRealEnElModeloAunqueNoHayaVeredicto() {
    ModelAndView mav = controlador.mostrarAsistente(6, null, requestMock);

    assertThat(mav.getModel().containsKey("gastoReal"), is(true));
  }

  @Test
  public void debePedirLosConsejosSobreElMismoAnalisisQueMuestraLaPantalla() {
    GastoReal gastoReal = sinDatos();
    when(servicioMock.calcular(usuario, 12)).thenReturn(gastoReal);

    controlador.mostrarAsistente(12, null, requestMock);

    verify(servicioConsejosMock).obtenerPendientes(usuario, gastoReal);
  }

  @Test
  public void debeAplicarElConsejoYVolverAlAsistenteConElMismoPeriodo() {
    ModelAndView mav = controlador.aplicarConsejo(7L, 12, requestMock);

    verify(servicioConsejosMock).aplicar(7L, usuario);
    assertThat(mav.getViewName(), equalTo("redirect:/asistente?periodo=12"));
  }

  @Test
  public void debeDescartarElConsejoYVolverAlAsistente() {
    ModelAndView mav = controlador.descartarConsejo(7L, null, requestMock);

    verify(servicioConsejosMock).descartar(7L, usuario);
    assertThat(mav.getViewName(), equalTo("redirect:/asistente?periodo=6"));
  }

  @Test
  public void debeVolverAlAsistenteSinRomperCuandoElConsejoAAplicarNoExiste() {
    doThrow(new ConsejoNoEncontrado()).when(servicioConsejosMock).aplicar(99L, usuario);

    ModelAndView mav = controlador.aplicarConsejo(99L, 6, requestMock);

    assertThat(
      mav.getViewName(),
      equalTo("redirect:/asistente?periodo=6&aviso=consejo-inexistente")
    );
  }

  @Test
  public void debeRedirigirAlLoginSinAplicarNadaCuandoSeEnviaLaAccionSinSesion() {
    when(requestMock.getSession(false)).thenReturn(null);

    ModelAndView mav = controlador.aplicarConsejo(7L, 6, requestMock);

    assertThat(mav.getViewName(), equalTo("redirect:/login"));
    verify(servicioConsejosMock, never()).aplicar(any(), any());
  }

  @Test
  public void debePonerEnElModeloSiFaltaLaSintesisParaQueLaVistaLaPidaAparte() {
    when(servicioConsejosMock.faltaLaSintesis(any())).thenReturn(true);

    ModelAndView mav = controlador.mostrarAsistente(6, null, requestMock);

    assertThat(mav.getModel().get("sintesisPendiente"), equalTo(true));
  }

  @Test
  public void debeDevolverLaSintesisComoTextoPlanoSobreElPeriodoPedido() {
    GastoReal gastoReal = sinDatos();
    when(servicioMock.calcular(usuario, 12)).thenReturn(gastoReal);
    when(servicioConsejosMock.sintetizar(usuario, gastoReal)).thenReturn("Tu año en una frase.");

    ResponseEntity<String> respuesta = controlador.sintesis(12, requestMock);

    assertThat(respuesta.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(respuesta.getBody(), equalTo("Tu año en una frase."));
  }

  @Test
  public void debeResponderSinContenidoCuandoNoHaySintesisNiCuandoFaltanLosIndicadores() {
    assertThat(
      controlador.sintesis(6, requestMock).getStatusCode(),
      equalTo(HttpStatus.NO_CONTENT)
    );

    when(servicioMock.calcular(any(), anyInt())).thenThrow(new IndicadoresNoDisponibles("sin red"));
    assertThat(
      controlador.sintesis(6, requestMock).getStatusCode(),
      equalTo(HttpStatus.NO_CONTENT)
    );
  }

  @Test
  public void debeResponderNoAutorizadoALaSintesisSinSesion() {
    when(sessionMock.getAttribute("USUARIO")).thenReturn(null);

    assertThat(
      controlador.sintesis(6, requestMock).getStatusCode(),
      equalTo(HttpStatus.UNAUTHORIZED)
    );
    verify(servicioConsejosMock, never()).sintetizar(any(), any());
  }

  @Test
  public void debePonerElPeriodoEnPalabrasYLosMontosVigentesParaLaPropuesta() {
    Veredicto veredicto = new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), 9.4, 7.6);
    when(servicioMock.calcular(any(), anyInt()))
      .thenReturn(
        new GastoReal(List.of(), veredicto, YearMonth.of(2026, 9), List.of(), null, true)
      );
    Categoria alimentacion = new Categoria();
    alimentacion.setNombre("Alimentación");
    when(servicioPresupuestoMock.obtenerDelMes(any(), any()))
      .thenReturn(List.of(new Presupuesto(usuario, alimentacion, YearMonth.now(), 180000.0)));

    ModelAndView mav = controlador.mostrarAsistente(6, null, requestMock);

    assertThat(mav.getModel().get("periodoTexto"), equalTo("Mayo a Septiembre 2026"));
    @SuppressWarnings("unchecked")
    Map<String, Double> montos = (Map<String, Double>) mav.getModel().get("montosActuales");
    assertThat(montos.get("Alimentación"), equalTo(180000.0));
    assertThat(mav.getModel().get("mesActualNombre"), notNullValue());
  }

  @Test
  public void debeEscribirElPeriodoConElAnioSoloUnaVezSiEsElMismo() {
    assertThat(
      ControladorAsistente.textoDelPeriodo(
        new Veredicto(YearMonth.of(2025, 11), YearMonth.of(2026, 9), 1.0, 1.0)
      ),
      equalTo("Noviembre 2025 a Septiembre 2026")
    );
  }

  private GastoReal sinDatos() {
    return new GastoReal(List.of(), null, null, List.of(), null, true);
  }
}
