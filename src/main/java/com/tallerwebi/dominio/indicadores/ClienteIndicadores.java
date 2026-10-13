package com.tallerwebi.dominio.indicadores;

import java.util.List;

/**
 * Acceso a la API externa de indicadores. La implementación vive en infraestructura;
 * el servicio solo conoce esta interfaz, así en los tests se reemplaza por un mock.
 */
public interface ClienteIndicadores {
  // Serie completa del dólar oficial, un registro por día
  List<CotizacionDolar> obtenerDolarOficialHistorico();

  CotizacionDolar obtenerDolarOficialDeHoy();

  // Un registro por mes publicado
  List<InflacionMensual> obtenerInflacionMensual();
}
