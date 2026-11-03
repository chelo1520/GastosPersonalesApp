package com.tallerwebi.infraestructura.indicadores;

import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import com.tallerwebi.dominio.indicadores.ClienteIndicadores;
import com.tallerwebi.dominio.indicadores.CotizacionDolar;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Indicadores de ArgentinaDatos (dólar histórico e inflación) y DolarApi (dólar de hoy). Ambas son
 * gratuitas, sin clave y devuelven JSON (decisión TEC-08). Esta clase hace la llamada HTTP; la
 * lectura de la respuesta la hace ParserIndicadores.
 */
@Component
public class ClienteArgentinaDatos implements ClienteIndicadores {

  private final String urlDolarHistorico;
  private final String urlInflacion;
  private final String urlDolarHoy;
  private final ParserIndicadores parser;
  private final HttpClient http;

  @Autowired
  public ClienteArgentinaDatos(
    @Value("${api.indicadores.dolar-historico.url}") String urlDolarHistorico,
    @Value("${api.indicadores.inflacion.url}") String urlInflacion,
    @Value("${api.indicadores.dolar-hoy.url}") String urlDolarHoy,
    ParserIndicadores parser
  ) {
    this(
      urlDolarHistorico,
      urlInflacion,
      urlDolarHoy,
      parser,
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    );
  }

  ClienteArgentinaDatos(
    String urlDolarHistorico,
    String urlInflacion,
    String urlDolarHoy,
    ParserIndicadores parser,
    HttpClient http
  ) {
    this.urlDolarHistorico = urlDolarHistorico;
    this.urlInflacion = urlInflacion;
    this.urlDolarHoy = urlDolarHoy;
    this.parser = parser;
    this.http = http;
  }

  @Override
  public List<CotizacionDolar> obtenerDolarOficialHistorico() {
    return parser.parsearDolares(consultar(urlDolarHistorico));
  }

  @Override
  public CotizacionDolar obtenerDolarOficialDeHoy() {
    return parser.parsearDolarDeHoy(consultar(urlDolarHoy));
  }

  @Override
  public List<InflacionMensual> obtenerInflacionMensual() {
    return parser.parsearInflacion(consultar(urlInflacion));
  }

  // Devuelve el JSON crudo, sin interpretarlo
  private String consultar(String url) {
    HttpRequest pedido = HttpRequest
      .newBuilder(URI.create(url))
      .timeout(Duration.ofSeconds(10))
      .header("Accept", "application/json")
      .GET()
      .build();
    try {
      HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
      if (respuesta.statusCode() != HttpStatus.OK.value()) {
        throw new IndicadoresNoDisponibles(url + " respondió " + respuesta.statusCode());
      }
      return respuesta.body();
    } catch (IOException e) {
      throw new IndicadoresNoDisponibles("No se pudo consultar " + url, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IndicadoresNoDisponibles("Consulta interrumpida a " + url, e);
    }
  }
}
