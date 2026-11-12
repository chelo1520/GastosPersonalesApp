package com.tallerwebi.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String email;
  private String password;
  private String rol;

  // Ingreso mensual y qué porcentaje de él se usa como presupuesto (null = todo)
  private Double ingresoMensual;
  private Double porcentajePresupuesto;
  private Integer guiaPaso;
  // Datos personales, opcionales: la app saluda por el nombre si está cargado
  private String nombre;
  private String apellido;
  private LocalDate fechaNacimiento;

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public void setApellido(String apellido) {
    this.apellido = apellido;
  }

  public LocalDate getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(LocalDate fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }

  public Integer getGuiaPaso() {
    return guiaPaso;
  }

  public void setGuiaPaso(Integer guiaPaso) {
    this.guiaPaso = guiaPaso;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRol() {
    return rol;
  }

  public void setRol(String rol) {
    this.rol = rol;
  }

  public Double getIngresoMensual() {
    return ingresoMensual;
  }

  public void setIngresoMensual(Double ingresoMensual) {
    this.ingresoMensual = ingresoMensual;
  }

  public Double getPorcentajePresupuesto() {
    return porcentajePresupuesto;
  }

  public void setPorcentajePresupuesto(Double porcentajePresupuesto) {
    this.porcentajePresupuesto = porcentajePresupuesto;
  }

  // Presupuesto mensual que sale del perfil; 0 si todavía no cargó su ingreso
  public Double presupuestoSegunPerfil() {
    if (ingresoMensual == null) {
      return 0.0;
    }
    double porcentaje = porcentajePresupuesto == null ? 100.0 : porcentajePresupuesto;
    return (ingresoMensual * porcentaje) / 100.0;
  }
}
