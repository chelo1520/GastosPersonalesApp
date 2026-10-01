package com.tallerwebi.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Gasto
 */
@Entity
public class Gasto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Double importe;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate fecha;

  private String descripcion;

  @ManyToOne
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  public Gasto() {}

  public Gasto(Double importe, LocalDate fecha, String descripcion) {
    this.importe = importe;
    this.fecha = fecha;
    this.descripcion = descripcion;
  }

  public Double getImporte() {
    return importe;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setImporte(Double importe) {
    this.importe = importe;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public Long getId() {
    return this.id;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public Usuario getUsuario() {
    return usuario;
  }
}
