package com.tallerwebi.dominio.comprobante;

import java.util.List;

/**
 * Datos propuestos a partir de un comprobante, pendientes de confirmación de la persona. Un ticket
 * con productos de varias categorías trae un gasto por categoría; un resumen de tarjeta, un gasto
 * por consumo; cualquier otro comprobante, un solo gasto.
 *
 * @param tipoOperacion operación identificada en el comprobante
 * @param tipoComprobante tipo de documento identificado
 * @param total importe total pagado según el comprobante, o null si no figura
 * @param gastos gastos propuestos, al menos uno
 */
public record DatosComprobante(
  String tipoOperacion,
  String tipoComprobante,
  Double total,
  List<GastoDelComprobante> gastos
) {
  public DatosComprobante {
    gastos = List.copyOf(gastos);
  }
}
