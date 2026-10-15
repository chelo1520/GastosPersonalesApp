package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.YearMonth;

/**
 * Un consejo del asistente para un usuario en un mes. Se persiste para recordar si el usuario
 * lo descartó o lo aplicó, y para guardar la síntesis del período (en el veredicto).
 */
@Entity
public class Consejo {

  // Prefijos del ancla: a qué apunta la lectura en el gráfico o en la tabla
  static final String ANCLA_MES = "mes:";
  static final String ANCLA_CATEGORIA = "categoria:";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  // "AAAA-MM": el mes en que se generó
  private String mes;

  // Como texto plano, sin ENUM ni CHECK de MySQL: así se pueden agregar tipos sin migrar la columna
  @Convert(converter = ConvertidorDeTipoConsejo.class)
  private TipoConsejo tipo;

  @Convert(converter = ConvertidorDeEstadoConsejo.class)
  private EstadoConsejo estado = EstadoConsejo.PENDIENTE;

  // Categoría a la que refiere (null si es un consejo general; en el veredicto, el período)
  private String categoria;

  // Hibernate deja un embebido en null cuando todas sus columnas son null: los getters lo contemplan
  @Embedded
  private Hechos hechos = new Hechos();

  @Embedded
  private Sintesis sintesis = new Sintesis();

  // "mes:2026-07" o "categoria:Servicios": dónde se señala la lectura; null si no apunta a nada
  private String ancla;

  // Monto propuesto para la acción "actualizar presupuesto" (null si no hay acción)
  private Double montoSugerido;

  public Consejo() {}

  public Consejo(
    Usuario usuario,
    YearMonth mes,
    TipoConsejo tipo,
    String categoria,
    String texto,
    Double montoSugerido
  ) {
    this.usuario = usuario;
    this.mes = mes.toString();
    this.tipo = tipo;
    this.categoria = categoria;
    this.hechos = new Hechos(null, texto);
    this.montoSugerido = montoSugerido;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public YearMonth getMes() {
    return YearMonth.parse(mes);
  }

  public TipoConsejo getTipo() {
    return tipo;
  }

  public Severidad getSeveridad() {
    return tipo.getSeveridad();
  }

  public EstadoConsejo getEstado() {
    return estado;
  }

  public void setEstado(EstadoConsejo estado) {
    this.estado = estado;
  }

  public String getCategoria() {
    return categoria;
  }

  public String getTitulo() {
    return hechos == null ? null : hechos.getTitulo();
  }

  public String getTexto() {
    return hechos == null ? null : hechos.getTexto();
  }

  // Los hechos se recalculan en cada visita; el estado y la síntesis se conservan
  public void setHechos(String titulo, String texto) {
    this.hechos = new Hechos(titulo, texto);
  }

  public Consejo conTitulo(String titulo) {
    this.hechos = new Hechos(titulo, getTexto());
    return this;
  }

  public Double getMontoSugerido() {
    return montoSugerido;
  }

  public void setMontoSugerido(Double montoSugerido) {
    this.montoSugerido = montoSugerido;
  }

  public String getAncla() {
    return ancla;
  }

  public Consejo ancladoAlMes(YearMonth mesSenialado) {
    this.ancla = ANCLA_MES + mesSenialado;
    return this;
  }

  public Consejo ancladoALaCategoria(String nombre) {
    this.ancla = ANCLA_CATEGORIA + nombre;
    return this;
  }

  public void setAncla(String ancla) {
    this.ancla = ancla;
  }

  // "2026-07" si la lectura señala un mes del gráfico; null si no
  public String getMesSenialado() {
    return ancla != null && ancla.startsWith(ANCLA_MES)
      ? ancla.substring(ANCLA_MES.length())
      : null;
  }

  // "Servicios" si la lectura señala una fila de la tabla de categorías; null si no
  public String getCategoriaSenialada() {
    return ancla != null && ancla.startsWith(ANCLA_CATEGORIA)
      ? ancla.substring(ANCLA_CATEGORIA.length())
      : null;
  }

  public String getTextoRedactado() {
    return sintesis == null ? null : sintesis.getTexto();
  }

  public String getFirmaHechos() {
    return sintesis == null ? null : sintesis.getFirma();
  }

  public void redactar(String textoRedactado, String firmaHechos) {
    this.sintesis = new Sintesis(textoRedactado, firmaHechos);
  }

  // Lo que ve el usuario: en el veredicto, la síntesis del asistente si la hay; si no, los hechos tal cual
  public String getTextoParaMostrar() {
    return getTextoRedactado() != null ? getTextoRedactado() : getTexto();
  }

  // Los hechos que importan para la síntesis; si cambian, la síntesis vieja ya no sirve
  public String firmaDeLosHechos() {
    return Integer.toHexString((getTexto() + "|" + montoSugerido).hashCode());
  }

  public boolean isAplicable() {
    return montoSugerido != null;
  }

  public boolean isVeredicto() {
    return tipo.isVeredicto();
  }

  public boolean isLectura() {
    return tipo.getFamilia() == Familia.LECTURA;
  }

  public boolean isPropuesta() {
    return tipo.getFamilia() == Familia.PROPUESTA;
  }
}
