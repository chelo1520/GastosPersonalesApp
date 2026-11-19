package com.tallerwebi.dominio.comprobante;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** El usuario ya cargó gastos de este mismo archivo y siguen guardados. */
public class ComprobanteYaCargado extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public ComprobanteYaCargado(LocalDate fecha, long cantidadGastos) {
    super(
      "Este comprobante ya lo cargaste el " +
      fecha.format(FORMATO_FECHA) +
      " (" +
      (cantidadGastos == 1 ? "1 gasto" : cantidadGastos + " gastos") +
      "). Si es otro ticket, sacale una foto nueva."
    );
  }
}
