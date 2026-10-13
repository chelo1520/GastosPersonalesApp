package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.YearMonth;

/**
 * Un consejo del asistente para un usuario en un mes. Se persiste para recordar si el usuario
 * lo descartó o lo aplicó, y para que HU-15 no vuelva a redactarlo cada vez.
 */
@Entity
public class Consejo {

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

  // Categoría a la que refiere (null si es un consejo general)
  private String categoria;

  // Texto de las reglas: los hechos, siempre disponibles
  private String texto;

  // Texto redactado por el asistente (HU-15); null si todavía no se redactó o no hay IA
  @Column(length = 1000)
  private String textoRedactado;

  // Con qué hechos se redactó: si cambian, se vuelve a redactar
  private String firmaHechos;

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
    this.texto = texto;
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

  public String getTexto() {
    return texto;
  }

  public void setTexto(String texto) {
    this.texto = texto;
  }

  public Double getMontoSugerido() {
    return montoSugerido;
  }

  public void setMontoSugerido(Double montoSugerido) {
    this.montoSugerido = montoSugerido;
  }

  public String getTextoRedactado() {
    return textoRedactado;
  }

  public String getFirmaHechos() {
    return firmaHechos;
  }

  public void redactar(String textoRedactado, String firmaHechos) {
    this.textoRedactado = textoRedactado;
    this.firmaHechos = firmaHechos;
  }

  // Lo que ve el usuario: en el veredicto, la síntesis del asistente si la hay; si no, los hechos tal cual
  public String getTextoParaMostrar() {
    return textoRedactado != null ? textoRedactado : texto;
  }

  // Los hechos que importan para la síntesis; si cambian, la síntesis vieja ya no sirve
  public String firmaDeLosHechos() {
    return Integer.toHexString((texto + "|" + montoSugerido).hashCode());
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
