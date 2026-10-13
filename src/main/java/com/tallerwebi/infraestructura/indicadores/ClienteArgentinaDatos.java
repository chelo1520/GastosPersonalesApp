package com.tallerwebi.infraestructura.indicadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Cliente HTTP de ArgentinaDatos (dólar histórico e inflación) y DolarApi (dólar de hoy).
 * Ambas son gratuitas, sin clave y devuelven JSON (decisión TEC-08).
 */
@Component
public class ClienteArgentinaDatos implements ClienteIndicadores {

  static final String URL_DOLAR_HISTORICO =
    "https://api.argentinadatos.com/v1/cotizaciones/dolares/oficial";
  static final String URL_INFLACION =
    "https://api.argentinadatos.com/v1/finanzas/indices/inflacion";
  static final String URL_DOLAR_HOY = "https://dolarapi.com/v1/dolares/oficial";
  private static final int HTTP_OK = 200;

  private final HttpClient http;
  private final ObjectMapper json = new ObjectMapper();

  public ClienteArgentinaDatos() {
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  @Override
  public List<CotizacionDolar> obtenerDolarOficialHistorico() {
    return parsearDolares(obtener(URL_DOLAR_HISTORICO));
  }

  @Override
  public CotizacionDolar obtenerDolarOficialDeHoy() {
    return parsearDolarDeHoy(obtener(URL_DOLAR_HOY));
  }

  @Override
  public List<InflacionMensual> obtenerInflacionMensual() {
    return parsearInflacion(obtener(URL_INFLACION));
  }

  // [{"casa":"oficial","compra":1485,"venta":1535,"fecha":"2026-10-13"}, ...]
  List<CotizacionDolar> parsearDolares(String cuerpo) {
    List<CotizacionDolar> cotizaciones = new ArrayList<>();
    for (JsonNode nodo : leer(cuerpo)) {
      cotizaciones.add(
        new CotizacionDolar(
          LocalDate.parse(nodo.get("fecha").asText()),
          nodo.get("compra").asDouble(),
          nodo.get("venta").asDouble()
        )
      );
    }
    return cotizaciones;
  }

  // {"moneda":"USD","casa":"oficial","compra":1485,"venta":1535,"fechaActualizacion":"2026-10-13T15:00:00.000Z"}
  CotizacionDolar parsearDolarDeHoy(String cuerpo) {
    JsonNode nodo = leer(cuerpo);
    LocalDate fecha = OffsetDateTime.parse(nodo.get("fechaActualizacion").asText()).toLocalDate();
    return new CotizacionDolar(fecha, nodo.get("compra").asDouble(), nodo.get("venta").asDouble());
  }

  // [{"fecha":"2026-08-31","valor":1.7}, ...]: un registro por mes, fechado el último día
  List<InflacionMensual> parsearInflacion(String cuerpo) {
    List<InflacionMensual> meses = new ArrayList<>();
    for (JsonNode nodo : leer(cuerpo)) {
      YearMonth mes = YearMonth.from(LocalDate.parse(nodo.get("fecha").asText()));
      meses.add(new InflacionMensual(mes, nodo.get("valor").asDouble(), false));
    }
    return meses;
  }

  private JsonNode leer(String cuerpo) {
    try {
      return json.readTree(cuerpo);
    } catch (IOException e) {
      throw new IndicadoresNoDisponibles("Respuesta inválida de la API de indicadores", e);
    }
  }

  private String obtener(String url) {
    HttpRequest pedido = HttpRequest
      .newBuilder(URI.create(url))
      .timeout(Duration.ofSeconds(10))
      .header("Accept", "application/json")
      .GET()
      .build();
    try {
      HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
      if (respuesta.statusCode() != HTTP_OK) {
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
