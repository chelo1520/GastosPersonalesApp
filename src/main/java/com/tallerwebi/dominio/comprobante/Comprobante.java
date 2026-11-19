package com.tallerwebi.dominio.comprobante;

import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Un comprobante del que se cargaron gastos. La huella identifica el archivo: un usuario no puede
 * tener dos comprobantes con la misma. La clave es dónde quedó el archivo en el almacén (R2), o
 * null si no se pudo guardar.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "huella" }))
public class Comprobante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(nullable = false, length = 64)
  private String huella;

  private String tipo;

  private String clave;

  private LocalDateTime fechaCarga;

  public Comprobante() {}

  public Comprobante(Usuario usuario, String huella, String tipo, LocalDateTime fechaCarga) {
    this.usuario = usuario;
    this.huella = huella;
    this.tipo = tipo;
    this.fechaCarga = fechaCarga;
  }

  public Long getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public String getHuella() {
    return huella;
  }

  public String getTipo() {
    return tipo;
  }

  public String getClave() {
    return clave;
  }

  public void setClave(String clave) {
    this.clave = clave;
  }

  public LocalDateTime getFechaCarga() {
    return fechaCarga;
  }

  public void setFechaCarga(LocalDateTime fechaCarga) {
    this.fechaCarga = fechaCarga;
  }
}
