package com.tallerwebi.presentacion;

import java.time.YearMonth;

/**
 * Desde qué vista se abrió el registro rápido (rediseño v2): a dónde se vuelve después de guardar,
 * a mano o con un comprobante. Solo estos valores: un "volver" desconocido no redirige a ningún lado.
 */
enum OrigenDelRegistro {
  RESUMEN("resumen", "redirect:/home", true),
  MIS_GASTOS("mis-gastos", "redirect:/mostrar-gastos", true),
  PRESUPUESTOS("presupuestos", "redirect:/presupuestos", true),
  // La simulación recuerda sus dos meses en la sesión: se vuelve sin parámetro
  SIMULACION("simulacion", "redirect:/simulacion", false);

  static final String PARAMETRO = "volver";

  private final String valor;
  private final String vista;
  private final boolean conMes;

  OrigenDelRegistro(String valor, String vista, boolean conMes) {
    this.valor = valor;
    this.vista = vista;
    this.conMes = conMes;
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
    if (!conMes || mes == null || mes.equals(actual)) {
      return vista;
    }
    return vista + "?mes=" + mes;
  }
}
