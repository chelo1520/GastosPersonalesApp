package com.tallerwebi.infraestructura.indicadores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import com.tallerwebi.dominio.indicadores.CotizacionDolar;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Convierte las respuestas de ArgentinaDatos y DolarApi en cotizaciones e inflación. */
@Component
public class ParserIndicadores {

  private final ObjectMapper json = new ObjectMapper();

  // [{"casa":"oficial","compra":1485,"venta":1535,"fecha":"2026-10-13"}, ...]
  public List<CotizacionDolar> parsearDolares(String cuerpo) {
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
  public CotizacionDolar parsearDolarDeHoy(String cuerpo) {
    JsonNode nodo = leer(cuerpo);
    LocalDate fecha = OffsetDateTime.parse(nodo.get("fechaActualizacion").asText()).toLocalDate();
    return new CotizacionDolar(fecha, nodo.get("compra").asDouble(), nodo.get("venta").asDouble());
  }

  // [{"fecha":"2026-08-31","valor":1.7}, ...]: un registro por mes, fechado el último día
  public List<InflacionMensual> parsearInflacion(String cuerpo) {
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
}
