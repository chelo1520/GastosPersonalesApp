package com.tallerwebi.dominio.indicadores;

import java.time.LocalDate;

/** Cotización del dólar oficial de un día (compra y venta en pesos). */
public final class CotizacionDolar {

  private final LocalDate fecha;
  private final Double compra;
  private final Double venta;

  public CotizacionDolar(LocalDate fecha, Double compra, Double venta) {
    this.fecha = fecha;
    this.compra = compra;
    this.venta = venta;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public Double getCompra() {
    return compra;
  }

  public Double getVenta() {
    return venta;
  }
}
