package com.tallerwebi.infraestructura.indicadores;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import com.tallerwebi.dominio.indicadores.ClienteIndicadores;
import com.tallerwebi.dominio.indicadores.CotizacionDolar;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El HttpClient y el parser son mocks: no hay red. */
public class ClienteArgentinaDatosTest {

  private HttpClient http;
  private ParserIndicadores parser;
  private ClienteIndicadores cliente;

  @BeforeEach
  void init() {
    http = mock(HttpClient.class);
    parser = mock(ParserIndicadores.class);
    cliente =
      new ClienteArgentinaDatos(
        "http://dolar-historico.test",
        "http://inflacion.test",
        "http://dolar-hoy.test",
        parser,
        http
      );
  }

  @Test
  void debeDevolverLaSerieDelDolarQueLeeElParserDeLaRespuesta() throws Exception {
    List<CotizacionDolar> serie = List.of(
      new CotizacionDolar(LocalDate.of(2026, 10, 13), 1485.0, 1535.0)
    );
    respondeLaApi(200, "[serie]");
    when(parser.parsearDolares("[serie]")).thenReturn(serie);

    assertThat(cliente.obtenerDolarOficialHistorico(), sameInstance(serie));
  }

  @Test
  void debeDevolverElDolarDeHoyQueLeeElParserDeLaRespuesta() throws Exception {
    CotizacionDolar hoy = new CotizacionDolar(LocalDate.of(2026, 10, 13), 1485.0, 1535.0);
    respondeLaApi(200, "{hoy}");
    when(parser.parsearDolarDeHoy("{hoy}")).thenReturn(hoy);

    assertThat(cliente.obtenerDolarOficialDeHoy(), sameInstance(hoy));
  }

  @Test
  void debeDevolverLaInflacionQueLeeElParserDeLaRespuesta() throws Exception {
    List<InflacionMensual> meses = List.of(new InflacionMensual(YearMonth.of(2026, 8), 1.7, false));
    respondeLaApi(200, "[meses]");
    when(parser.parsearInflacion("[meses]")).thenReturn(meses);

    assertThat(cliente.obtenerInflacionMensual(), sameInstance(meses));
  }

  @Test
  void debeAvisarQueNoHayIndicadoresCuandoLaApiNoRespondeOk() throws Exception {
    respondeLaApi(503, "caída");

    assertThrows(IndicadoresNoDisponibles.class, () -> cliente.obtenerInflacionMensual());
  }

  @Test
  void debeAvisarQueNoHayIndicadoresCuandoNoHayRed() throws Exception {
    doThrow(new IOException("sin red")).when(http).send(any(), any());

    assertThrows(IndicadoresNoDisponibles.class, () -> cliente.obtenerDolarOficialDeHoy());
  }

  private void respondeLaApi(int estado, String cuerpo) throws Exception {
    @SuppressWarnings("unchecked")
    HttpResponse<String> respuesta = mock(HttpResponse.class);
    when(respuesta.statusCode()).thenReturn(estado);
    when(respuesta.body()).thenReturn(cuerpo);
    doReturn(respuesta).when(http).send(any(), any());
  }
}
