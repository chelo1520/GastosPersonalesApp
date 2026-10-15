package com.tallerwebi.dominio.comprobante;

/**
 * Indica qué campos requieren especial atención durante la revisión del comprobante.
 *
 * @param importeBajaConfianza el importe fue leído con baja confianza
 * @param fechaBajaConfianza la fecha fue leída con baja confianza
 * @param descripcionBajaConfianza la descripción fue leída con baja confianza
 * @param categoriaBajaConfianza la categoría fue sugerida con baja confianza
 */
public record ConfianzaComprobante(
  boolean importeBajaConfianza,
  boolean fechaBajaConfianza,
  boolean descripcionBajaConfianza,
  boolean categoriaBajaConfianza
) {}
