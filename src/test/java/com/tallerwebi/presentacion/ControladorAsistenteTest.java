package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.ServicioGastoReal;
import com.tallerwebi.dominio.asistente.Veredicto;
import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorAsistenteTest {

  private ServicioGastoReal servicioMock;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private Usuario usuario;
  private ControladorAsistente controlador;

  @BeforeEach
  public void init() {
    servicioMock = mock(ServicioGastoReal.class);
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    usuario = new Usuario();
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("USUARIO")).thenReturn(usuario);
    when(servicioMock.calcular(any(), anyInt())).thenReturn(sinDatos());
    controlador = new ControladorAsistente(servicioMock);
  }

  @Test
  public void debeRedirigirAlLoginSinCalcularNadaCuandoNoHayUsuarioEnSesion() {
    when(sessionMock.getAttribute("USUARIO")).thenReturn(null);

    ModelAndView mav = controlador.mostrarAsistente(6, requestMock);

    assertThat(mav.getViewName(), equalTo("redirect:/login"));
    verify(servicioMock, never()).calcular(any(), anyInt());
  }

  @Test
  public void debeUsarSeisMesesCuandoNoSePideUnPeriodo() {
    ModelAndView mav = controlador.mostrarAsistente(null, requestMock);

    assertThat(mav.getViewName(), equalTo("asistente"));
    assertThat(mav.getModel().get("periodo"), equalTo(6));
    verify(servicioMock).calcular(usuario, 6);
  }

  @Test
  public void debeUsarElPeriodoPedidoCuandoEsUnoDeLosPermitidos() {
    controlador.mostrarAsistente(12, requestMock);

    verify(servicioMock).calcular(usuario, 12);
  }

  @Test
  public void debeCaerEnSeisMesesCuandoElPeriodoPedidoNoEstaPermitido() {
    controlador.mostrarAsistente(7, requestMock);

    verify(servicioMock).calcular(usuario, 6);
  }

  @Test
  public void debeAvisarSinRomperCuandoLosIndicadoresNoEstanDisponibles() {
    when(servicioMock.calcular(any(), anyInt())).thenThrow(new IndicadoresNoDisponibles("sin red"));

    ModelAndView mav = controlador.mostrarAsistente(6, requestMock);

    assertThat(mav.getViewName(), equalTo("asistente"));
    assertThat(mav.getModel().get("indicadoresNoDisponibles"), equalTo(true));
    assertThat(mav.getModel().get("gastoReal"), nullValue());
  }

  @Test
  public void debeRedactarElVeredictoConSubidaNominalInflacionYDiferenciaReal() {
    Veredicto veredicto = new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), 25.0, 21.0);

    String texto = ControladorAsistente.textoDelVeredicto(veredicto);

    assertThat(texto, containsString("Desde Mayo 2026 tus gastos subieron 25,0 %"));
    assertThat(texto, containsString("La inflación fue 21,0 %"));
    assertThat(texto, containsString("gastás 3,3 % más en serio."));
  }

  @Test
  public void debeDecirQueGastaMenosEnSerioCuandoLaInflacionSuperaLaSubidaNominal() {
    Veredicto veredicto = new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), 10.0, 21.0);

    String texto = ControladorAsistente.textoDelVeredicto(veredicto);

    assertThat(texto, containsString("gastás 9,1 % menos en serio."));
  }

  @Test
  public void debeDejarElVeredictoVacioCuandoNoHayVeredicto() {
    ModelAndView mav = controlador.mostrarAsistente(6, requestMock);

    assertThat(mav.getModel().get("veredictoTexto"), nullValue());
    assertThat(mav.getModel().containsKey("gastoReal"), is(true));
  }

  private GastoReal sinDatos() {
    return new GastoReal(List.of(), null, null, List.of(), null, true);
  }
}
