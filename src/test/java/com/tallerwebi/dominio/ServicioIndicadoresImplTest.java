package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import com.tallerwebi.dominio.indicadores.ClienteIndicadores;
import com.tallerwebi.dominio.indicadores.CotizacionDolar;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import com.tallerwebi.dominio.indicadores.ServicioIndicadores;
import com.tallerwebi.dominio.indicadores.ServicioIndicadoresImpl;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioIndicadoresImplTest {

  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
  // "Hoy": martes 13 de octubre de 2026; el reloj se puede adelantar para simular otro día
  private static final Instant HOY = Instant.parse("2026-10-13T15:00:00Z");

  private ClienteIndicadores clienteMock;
  private RelojAjustable reloj;
  private ServicioIndicadores servicio;

  @BeforeEach
  public void init() {
    clienteMock = mock(ClienteIndicadores.class);
    reloj = new RelojAjustable(HOY);
    servicio = new ServicioIndicadoresImpl(clienteMock, reloj);
    conSeries();
  }

  @Test
  void debeDevolverLaCotizacionDelDiaCuandoHayDatoParaEsaFecha() {
    CotizacionDolar cotizacion = servicio.cotizacionDolar(LocalDate.of(2026, 10, 9));

    assertThat(cotizacion.getFecha(), equalTo(LocalDate.of(2026, 10, 9)));
    assertThat(cotizacion.getVenta(), equalTo(1520.0));
  }

  @Test
  void debeDevolverLaCotizacionDelDiaAnteriorConDatoCuandoLaFechaCaeEnFinDeSemana() {
    // Sábado 10 de octubre: la serie no tiene dato, se usa el viernes 9
    CotizacionDolar cotizacion = servicio.cotizacionDolar(LocalDate.of(2026, 10, 10));

    assertThat(cotizacion.getFecha(), equalTo(LocalDate.of(2026, 10, 9)));
  }

  @Test
  void debeLanzarIndicadoresNoDisponiblesCuandoLaFechaEsAnteriorALaSerie() {
    assertThrows(
      IndicadoresNoDisponibles.class,
      () -> servicio.cotizacionDolar(LocalDate.of(2020, 1, 1))
    );
  }

  @Test
  void debeDevolverElDolarDeHoyDeLaApiEnVivoCuandoEstaDisponible() {
    CotizacionDolar hoy = servicio.dolarDeHoy();

    assertThat(hoy.getFecha(), equalTo(LocalDate.of(2026, 10, 13)));
    assertThat(hoy.getVenta(), equalTo(1535.0));
  }

  @Test
  void debeDevolverLaInflacionPublicadaCuandoElMesTieneDato() {
    InflacionMensual agosto = servicio.inflacionMensual(YearMonth.of(2026, 8));

    assertThat(agosto.getPorcentaje(), equalTo(1.7));
    assertThat(agosto.isEstimada(), is(false));
  }

  @Test
  void debeDevolverElUltimoDatoComoEstimadoCuandoElMesTodaviaNoFuePublicado() {
    InflacionMensual octubre = servicio.inflacionMensual(YearMonth.of(2026, 10));

    assertThat(octubre.getMes(), equalTo(YearMonth.of(2026, 10)));
    assertThat(octubre.getPorcentaje(), equalTo(1.7));
    assertThat(octubre.isEstimada(), is(true));
  }

  @Test
  void debeConsultarLaApiUnaSolaVezCuandoSePidenVariosDatosElMismoDia() {
    servicio.cotizacionDolar(LocalDate.of(2026, 10, 9));
    servicio.inflacionMensual(YearMonth.of(2026, 8));
    servicio.dolarDeHoy();

    verify(clienteMock, times(1)).obtenerDolarOficialHistorico();
    verify(clienteMock, times(1)).obtenerInflacionMensual();
    verify(clienteMock, times(1)).obtenerDolarOficialDeHoy();
    assertThat(servicio.datosActualizados(), is(true));
    assertThat(servicio.ultimaActualizacion(), notNullValue());
  }

  @Test
  void debeVolverAConsultarLaApiCuandoCambiaElDia() {
    servicio.cotizacionDolar(LocalDate.of(2026, 10, 9));
    reloj.adelantar(Duration.ofDays(1));

    servicio.cotizacionDolar(LocalDate.of(2026, 10, 9));

    verify(clienteMock, times(2)).obtenerDolarOficialHistorico();
  }

  @Test
  void debeSeguirRespondiendoConLoCacheadoYAvisarCuandoLaApiFallaAlDiaSiguiente() {
    servicio.cotizacionDolar(LocalDate.of(2026, 10, 9));
    when(clienteMock.obtenerDolarOficialHistorico())
      .thenThrow(new IndicadoresNoDisponibles("sin red"));
    reloj.adelantar(Duration.ofDays(1));

    CotizacionDolar cotizacion = servicio.cotizacionDolar(LocalDate.of(2026, 10, 9));

    assertThat(cotizacion.getVenta(), equalTo(1520.0));
    assertThat(servicio.datosActualizados(), is(false));
    assertThat(servicio.ultimaActualizacion(), notNullValue());
  }

  @Test
  void debeLanzarIndicadoresNoDisponiblesCuandoLaApiFallaYNoHayNadaCacheado() {
    when(clienteMock.obtenerDolarOficialHistorico())
      .thenThrow(new IndicadoresNoDisponibles("sin red"));
    ServicioIndicadores sinCache = new ServicioIndicadoresImpl(clienteMock, reloj);

    assertThrows(
      IndicadoresNoDisponibles.class,
      () -> sinCache.cotizacionDolar(LocalDate.of(2026, 10, 9))
    );
    assertThat(sinCache.ultimaActualizacion(), nullValue());
    assertThat(sinCache.datosActualizados(), is(false));
  }

  private void conSeries() {
    when(clienteMock.obtenerDolarOficialHistorico())
      .thenReturn(
        List.of(
          new CotizacionDolar(LocalDate.of(2026, 10, 8), 1480.0, 1515.0),
          new CotizacionDolar(LocalDate.of(2026, 10, 9), 1485.0, 1520.0),
          new CotizacionDolar(LocalDate.of(2026, 10, 13), 1485.0, 1535.0)
        )
      );
    when(clienteMock.obtenerDolarOficialDeHoy())
      .thenReturn(new CotizacionDolar(LocalDate.of(2026, 10, 13), 1485.0, 1535.0));
    when(clienteMock.obtenerInflacionMensual())
      .thenReturn(
        List.of(
          new InflacionMensual(YearMonth.of(2026, 7), 2.1, false),
          new InflacionMensual(YearMonth.of(2026, 8), 1.7, false)
        )
      );
  }

  // Reloj que se puede adelantar dentro del test para simular el paso de los días
  private static final class RelojAjustable extends Clock {

    private Instant ahora;

    RelojAjustable(Instant inicio) {
      this.ahora = inicio;
    }

    void adelantar(Duration cuanto) {
      ahora = ahora.plus(cuanto);
    }

    @Override
    public ZoneId getZone() {
      return ZONA;
    }

    @Override
    public Clock withZone(ZoneId zona) {
      return this;
    }

    @Override
    public Instant instant() {
      return ahora;
    }
  }
}
