package com.tallerwebi.dominio.comprobante;

import java.time.LocalDate;

/**
 * Datos propuestos a partir de un comprobante, pendientes de confirmación de la persona.
 *
 * @param importe importe detectado
 * @param fecha fecha detectada
 * @param descripcion comercio o destinatario detectado
 * @param categoria nombre de categoría sugerido
 */
public record DatosComprobante(
  Double importe,
  LocalDate fecha,
  String descripcion,
  String categoria
) {}
