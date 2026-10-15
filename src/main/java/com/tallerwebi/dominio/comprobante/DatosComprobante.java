package com.tallerwebi.dominio.comprobante;

import java.time.LocalDate;

/**
 * Datos propuestos a partir de un comprobante, pendientes de confirmación de la persona.
 *
 * @param importe importe detectado
 * @param fecha fecha detectada
 * @param descripcion comercio o destinatario detectado
 * @param categoria nombre de categoría sugerido
 * @param tipoOperacion operación identificada en el comprobante
 * @param tipoComprobante tipo de documento identificado
 * @param confianza confianza estimada para cada dato extraído
 */
public record DatosComprobante(
  Double importe,
  LocalDate fecha,
  String descripcion,
  String categoria,
  String tipoOperacion,
  String tipoComprobante,
  ConfianzaComprobante confianza
) {}
