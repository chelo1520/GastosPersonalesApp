package com.tallerwebi.presentacion;

/**
 * Datos del formulario "Definir presupuesto"
 */
public class DatosPresupuesto {

  private Long categoriaId;
  private Double monto;
  private String mes;
  private String tipo;
  private Double valor;

  public DatosPresupuesto() {}

  public DatosPresupuesto(Long categoriaId, Double monto, String mes) {
    this.categoriaId = categoriaId;
    this.monto = monto;
    this.mes = mes;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }

  public void setCategoriaId(Long categoriaId) {
    this.categoriaId = categoriaId;
  }

  public Double getMonto() {
    return monto;
  }

  public void setMonto(Double monto) {
    this.monto = monto;
  }

  public String getMes() {
    return mes;
  }

  public void setMes(String mes) {
    this.mes = mes;
  }

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public Double getValor() {
    return valor;
  }

  public void setValor(Double valor) {
    this.valor = valor;
  }
}
