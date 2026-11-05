package com.tallerwebi.dominio.comprobante;

import java.time.LocalDate;

/**
 * Un gasto propuesto a partir de un comprobante.
 *
 * @param importe importe detectado
 * @param fecha fecha detectada, o null si no se pudo leer
 * @param descripcion comercio o destinatario detectado
 * @param detalle qué incluye (por ejemplo, "Leche, pan"), o null
 * @param categoria nombre de categoría sugerido, o null
 * @param confianza confianza estimada para cada dato extraído
 */
public record GastoDelComprobante(
  Double importe,
  LocalDate fecha,
  String descripcion,
  String detalle,
  String categoria,
  ConfianzaComprobante confianza
) {}
