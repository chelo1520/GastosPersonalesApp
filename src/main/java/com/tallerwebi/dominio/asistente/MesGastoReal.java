package com.tallerwebi.dominio.asistente;

import java.time.YearMonth;

/** Un mes del período: lo gastado, en pesos de hoy y en dólares, con el IPC que lo ajusta. */
public final class MesGastoReal {

  private final YearMonth mes;
  private final Double gastado;
  private final Double gastadoEnPesosDeHoy;
  private final Double ipc;
  private final boolean ipcEstimado;
  private final Double dolarCierre;
  private final boolean enCurso;

  public MesGastoReal(
    YearMonth mes,
    Double gastado,
    Double gastadoEnPesosDeHoy,
    Double ipc,
    boolean ipcEstimado,
    Double dolarCierre,
    boolean enCurso
  ) {
    this.mes = mes;
    this.gastado = gastado;
    this.gastadoEnPesosDeHoy = gastadoEnPesosDeHoy;
    this.ipc = ipc;
    this.ipcEstimado = ipcEstimado;
    this.dolarCierre = dolarCierre;
    this.enCurso = enCurso;
  }

  public YearMonth getMes() {
    return mes;
  }

  public Double getGastado() {
    return gastado;
  }

  public Double getGastadoEnPesosDeHoy() {
    return gastadoEnPesosDeHoy;
  }

  public Double getIpc() {
    return ipc;
  }

  public boolean isIpcEstimado() {
    return ipcEstimado;
  }

  public Double getDolarCierre() {
    return dolarCierre;
  }

  public Double getGastadoUsd() {
    return dolarCierre > 0 ? gastado / dolarCierre : 0.0;
  }

  public boolean isEnCurso() {
    return enCurso;
  }
}
