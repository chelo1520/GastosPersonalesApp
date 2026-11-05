package com.tallerwebi.dominio.categoria;

import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Cómo trata un usuario a una categoría del maestro: esencial (sus gastos nunca son hormiga) o
 * prescindible. Sin preferencia vale lo que dice la categoría. Las fijas no se pueden cambiar.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "categoria_id" }))
public class PreferenciaCategoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  @ManyToOne
  @JoinColumn(name = "categoria_id")
  private Categoria categoria;

  private Boolean esencial;

  public PreferenciaCategoria() {}

  public PreferenciaCategoria(Usuario usuario, Categoria categoria, Boolean esencial) {
    this.usuario = usuario;
    this.categoria = categoria;
    this.esencial = esencial;
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

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public Categoria getCategoria() {
    return categoria;
  }

  public void setCategoria(Categoria categoria) {
    this.categoria = categoria;
  }

  public Boolean getEsencial() {
    return esencial;
  }

  public void setEsencial(Boolean esencial) {
    this.esencial = esencial;
  }
}
