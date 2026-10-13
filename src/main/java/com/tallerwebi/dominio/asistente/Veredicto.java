package com.tallerwebi.dominio.asistente;

import java.time.YearMonth;

/** Del primer al último mes cerrado del período: cuánto subió el gasto, cuánto la inflación y la diferencia. */
public final class Veredicto {

  private final YearMonth desde;
  private final YearMonth hasta;
  private final Double variacionNominal;
  private final Double inflacionAcumulada;
  private final Double variacionReal;
  private final EstadoGastoReal estado;

  public Veredicto(
    YearMonth desde,
    YearMonth hasta,
    Double variacionNominal,
    Double inflacionAcumulada
  ) {
    this.desde = desde;
    this.hasta = hasta;
    this.variacionNominal = variacionNominal;
    this.inflacionAcumulada = inflacionAcumulada;
    this.variacionReal =
      CategoriaGastoReal.calcularVariacionReal(variacionNominal, inflacionAcumulada);
    this.estado = EstadoGastoReal.de(variacionReal);
  }

  public YearMonth getDesde() {
    return desde;
  }

  public YearMonth getHasta() {
    return hasta;
  }

  public Double getVariacionNominal() {
    return variacionNominal;
  }

  public Double getInflacionAcumulada() {
    return inflacionAcumulada;
  }

  public Double getVariacionReal() {
    return variacionReal;
  }

  public EstadoGastoReal getEstado() {
    return estado;
  }
}
