package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Enumerated(EnumType.STRING)
  private TipoConsejo tipo;

  @Enumerated(EnumType.STRING)
  private Severidad severidad;

  @Enumerated(EnumType.STRING)
  private EstadoConsejo estado = EstadoConsejo.PENDIENTE;

  // Categoría a la que refiere (null si es un consejo general)
  private String categoria;

  private String texto;

  // Monto propuesto para la acción "actualizar presupuesto" (null si no hay acción)
  private Double montoSugerido;

  public Consejo() {}

  public Consejo(
    Usuario usuario,
    YearMonth mes,
    TipoConsejo tipo,
    Severidad severidad,
    String categoria,
    String texto,
    Double montoSugerido
  ) {
    this.usuario = usuario;
    this.mes = mes.toString();
    this.tipo = tipo;
    this.severidad = severidad;
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
    return severidad;
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

  public boolean isAplicable() {
    return montoSugerido != null;
  }
}
