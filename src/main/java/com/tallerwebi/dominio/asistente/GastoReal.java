package com.tallerwebi.dominio.asistente;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Resultado completo del análisis de un período para el asistente. */
public final class GastoReal {

  private final List<MesGastoReal> meses;
  private final Veredicto veredicto;
  private final YearMonth ultimoMesCerrado;
  private final List<CategoriaGastoReal> categorias;
  private final LocalDateTime indicadoresActualizadosEn;
  private final boolean indicadoresAlDia;

  public GastoReal(
    List<MesGastoReal> meses,
    Veredicto veredicto,
    YearMonth ultimoMesCerrado,
    List<CategoriaGastoReal> categorias,
    LocalDateTime indicadoresActualizadosEn,
    boolean indicadoresAlDia
  ) {
    this.meses = meses;
    this.veredicto = veredicto;
    this.ultimoMesCerrado = ultimoMesCerrado;
    this.categorias = categorias;
    this.indicadoresActualizadosEn = indicadoresActualizadosEn;
    this.indicadoresAlDia = indicadoresAlDia;
  }

  public List<MesGastoReal> getMeses() {
    return meses;
  }

  // null cuando el período no tiene dos meses cerrados con gastos para comparar
  public Veredicto getVeredicto() {
    return veredicto;
  }

  public YearMonth getUltimoMesCerrado() {
    return ultimoMesCerrado;
  }

  public List<CategoriaGastoReal> getCategorias() {
    return categorias;
  }

  public LocalDateTime getIndicadoresActualizadosEn() {
    return indicadoresActualizadosEn;
  }

  public boolean isIndicadoresAlDia() {
    return indicadoresAlDia;
  }

  public Double getGastadoUsdDelUltimoMesCerrado() {
    for (MesGastoReal mes : meses) {
      if (mes.getMes().equals(ultimoMesCerrado)) {
        return mes.getGastadoUsd();
      }
    }
    return 0.0;
  }
}
