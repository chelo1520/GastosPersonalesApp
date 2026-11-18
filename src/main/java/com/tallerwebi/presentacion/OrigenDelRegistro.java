package com.tallerwebi.presentacion;

import java.time.YearMonth;

/**
 * Desde qué vista se abrió el registro rápido (rediseño v2): a dónde se vuelve después de guardar,
 * a mano o con un comprobante. Solo estos valores: un "volver" desconocido no redirige a ningún lado.
 */
enum OrigenDelRegistro {
  RESUMEN("resumen", "redirect:/home"),
  MIS_GASTOS("mis-gastos", "redirect:/mostrar-gastos"),
  PRESUPUESTOS("presupuestos", "redirect:/presupuestos");

  static final String PARAMETRO = "volver";

  private final String valor;
  private final String vista;

  OrigenDelRegistro(String valor, String vista) {
    this.valor = valor;
    this.vista = vista;
  }

  // null si no viene o no es uno de los orígenes conocidos
  static OrigenDelRegistro de(String valor) {
    for (OrigenDelRegistro origen : values()) {
      if (origen.valor.equals(valor)) {
        return origen;
      }
    }
    return null;
  }

  String getValor() {
    return valor;
  }

  // La vista de origen en el mes indicado; el mes actual va sin parámetro
  String redireccion(YearMonth mes, YearMonth actual) {
    if (mes == null || mes.equals(actual)) {
      return vista;
    }
    return vista + "?mes=" + mes;
  }
}
