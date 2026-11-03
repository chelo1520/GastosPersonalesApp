package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import com.tallerwebi.dominio.gasto.Gasto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Un renglón de la revisión del comprobante. La persona edita los datos del gasto y si se incluye;
 * el detalle y las marcas de "revisar" solo se muestran (no vuelven en el formulario).
 */
public class GastoPropuesto {

  private boolean incluir = true;
  private Double importe;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate fecha;

  private String descripcion;
  private Long categoriaId;
  private String detalle;
  private boolean revisarImporte;
  private boolean revisarFecha;
  private boolean revisarDescripcion;
  private boolean revisarCategoria;

  public GastoPropuesto() {}

  // La categoría sugerida tiene que ser una de las existentes; si no, la elige la persona
  static GastoPropuesto de(GastoDelComprobante leido, List<Categoria> categorias) {
    Long categoriaSugerida = categorias
      .stream()
      .filter(categoria -> leido.categoria() != null)
      .filter(categoria -> categoria.getNombre().equalsIgnoreCase(leido.categoria().trim()))
      .map(Categoria::getId)
      .findFirst()
      .orElse(null);

    GastoPropuesto propuesto = new GastoPropuesto();
    propuesto.importe = leido.importe();
    propuesto.fecha = leido.fecha();
    propuesto.descripcion = leido.descripcion();
    propuesto.categoriaId = categoriaSugerida;
    propuesto.detalle = leido.detalle();
    propuesto.revisarImporte = leido.confianza().importeBajaConfianza();
    propuesto.revisarFecha = leido.confianza().fechaBajaConfianza();
    propuesto.revisarDescripcion = leido.confianza().descripcionBajaConfianza();
    propuesto.revisarCategoria =
      categoriaSugerida == null || leido.confianza().categoriaBajaConfianza();
    return propuesto;
  }

  Gasto aGasto(Categoria categoria) {
    Gasto gasto = new Gasto(importe, fecha, descripcion);
    gasto.setCategoria(categoria);
    return gasto;
  }

  public boolean isIncluir() {
    return incluir;
  }

  public void setIncluir(boolean incluir) {
    this.incluir = incluir;
  }

  public Double getImporte() {
    return importe;
  }

  public void setImporte(Double importe) {
    this.importe = importe;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }

  public void setCategoriaId(Long categoriaId) {
    this.categoriaId = categoriaId;
  }

  public String getDetalle() {
    return detalle;
  }

  public boolean isRevisarImporte() {
    return revisarImporte;
  }

  public boolean isRevisarFecha() {
    return revisarFecha;
  }

  public boolean isRevisarDescripcion() {
    return revisarDescripcion;
  }

  public boolean isRevisarCategoria() {
    return revisarCategoria;
  }
}
