package com.tallerwebi.dominio.simulacion;

import java.util.List;

public record EstadoSimulacion(
  List<Long> excluidos,
  List<GastoSimulado> agregados,
  String descripcionExtra,
  String montoExtra,
  String descripcionOrdinario,
  String montoOrdinario
) {
  public record GastoSimulado(String descripcion, Double importe, String tipo, boolean incluido) {}

  public static EstadoSimulacion vacio() {
    return new EstadoSimulacion(List.of(), List.of(), "", "", "", "");
  }
}
