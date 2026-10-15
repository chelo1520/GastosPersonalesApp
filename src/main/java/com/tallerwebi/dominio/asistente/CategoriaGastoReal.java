package com.tallerwebi.dominio.asistente;

/** Una categoría en el último mes cerrado, comparada con el mes anterior y con el IPC. */
public final class CategoriaGastoReal {

  private final String nombre;
  private final Double gastado;
  private final Double gastadoAnterior;
  private final Double variacionNominal;
  private final Double ipc;
  private final Double variacionReal;
  private final Diagnostico diagnostico;

  public CategoriaGastoReal(String nombre, Double gastado, Double gastadoAnterior, Double ipc) {
    this.nombre = nombre;
    this.gastado = gastado;
    this.gastadoAnterior = gastadoAnterior;
    this.ipc = ipc;
    this.variacionNominal = calcularVariacionNominal(gastado, gastadoAnterior);
    this.variacionReal = calcularVariacionReal(variacionNominal, ipc);
    this.diagnostico =
      gastadoAnterior == 0 && gastado > 0
        ? Diagnostico.NUEVO
        : Diagnostico.de(variacionNominal, variacionReal);
  }

  private static Double calcularVariacionNominal(Double gastado, Double anterior) {
    if (anterior == null || anterior <= 0) {
      return null;
    }
    return (gastado / anterior - 1) * 100;
  }

  // Descuenta la inflación: lo que varió en términos reales, más allá de que los precios subieron
  static Double calcularVariacionReal(Double variacionNominal, Double ipc) {
    if (variacionNominal == null || ipc == null) {
      return null;
    }
    return ((1 + variacionNominal / 100) / (1 + ipc / 100) - 1) * 100;
  }

  public String getNombre() {
    return nombre;
  }

  public Double getGastado() {
    return gastado;
  }

  public Double getGastadoAnterior() {
    return gastadoAnterior;
  }

  // null cuando no hay gasto en el mes anterior para comparar
  public Double getVariacionNominal() {
    return variacionNominal;
  }

  public Double getIpc() {
    return ipc;
  }

  public Double getVariacionReal() {
    return variacionReal;
  }

  public Diagnostico getDiagnostico() {
    return diagnostico;
  }
}
