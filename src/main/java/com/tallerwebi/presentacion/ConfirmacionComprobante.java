package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import java.util.ArrayList;
import java.util.List;

/**
 * El formulario de revisión de un comprobante: los gastos propuestos y los datos del comprobante
 * que se muestran arriba (viajan ocultos para poder volver a mostrar la revisión si hay un error).
 */
public class ConfirmacionComprobante {

  private String tipoOperacion;
  private String tipoComprobante;
  private Double total;
  private List<GastoPropuesto> gastos = new ArrayList<>();

  public ConfirmacionComprobante() {}

  static ConfirmacionComprobante de(DatosComprobante datos, List<Categoria> categorias) {
    ConfirmacionComprobante confirmacion = new ConfirmacionComprobante();
    confirmacion.tipoOperacion = datos.tipoOperacion();
    confirmacion.tipoComprobante = datos.tipoComprobante();
    confirmacion.total = datos.total();
    for (var gasto : datos.gastos()) {
      confirmacion.gastos.add(GastoPropuesto.de(gasto, categorias));
    }
    return confirmacion;
  }

  public String getTipoOperacion() {
    return tipoOperacion;
  }

  public void setTipoOperacion(String tipoOperacion) {
    this.tipoOperacion = tipoOperacion;
  }

  public String getTipoComprobante() {
    return tipoComprobante;
  }

  public void setTipoComprobante(String tipoComprobante) {
    this.tipoComprobante = tipoComprobante;
  }

  public Double getTotal() {
    return total;
  }

  public void setTotal(Double total) {
    this.total = total;
  }

  public List<GastoPropuesto> getGastos() {
    return gastos;
  }

  public void setGastos(List<GastoPropuesto> gastos) {
    this.gastos = gastos;
  }
}
