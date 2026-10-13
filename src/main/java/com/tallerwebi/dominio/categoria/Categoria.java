package com.tallerwebi.dominio.categoria;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Categoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String descripcion;
  private String nombre;

  // Una categoría sin marcar se trata como esencial: ante la duda, no se cuenta como gasto hormiga
  private Boolean esencial;

  // Las básicas (vivienda, salud, servicios...) son siempre esenciales: el usuario no las puede
  // marcar como hormiga
  private Boolean fija;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public boolean isEsencial() {
    return isFija() || !Boolean.FALSE.equals(esencial);
  }

  public void setEsencial(Boolean esencial) {
    this.esencial = esencial;
  }

  public boolean isFija() {
    return Boolean.TRUE.equals(fija);
  }

  public void setFija(Boolean fija) {
    this.fija = fija;
  }
}
