package com.tallerwebi.dominio.indicadores;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * ServicioIndicadores: dólar e inflación para el resto de la app, con caché y fallback
 */
public interface ServicioIndicadores {
  // Cotización del día; si ese día no hay dato (fin de semana, feriado), la del día anterior con dato
  CotizacionDolar cotizacionDolar(LocalDate fecha);

  CotizacionDolar dolarDeHoy();

  // Si el mes todavía no fue publicado, devuelve el último conocido marcado como estimado
  InflacionMensual inflacionMensual(YearMonth mes);

  // null si nunca se pudo cargar nada
  LocalDateTime ultimaActualizacion();

  // false cuando se está respondiendo con datos cacheados porque la API no respondió hoy
  boolean datosActualizados();
}
