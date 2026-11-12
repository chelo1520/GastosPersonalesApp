package com.tallerwebi.presentacion;

/**
 * Formulario de datos personales del perfil. La fecha viaja como texto ISO (AAAA-MM-DD) y el
 * controlador la convierte; vacía es "sin fecha".
 */
public class DatosPersonales {

  private String nombre;
  private String apellido;
  private String fechaNacimiento;

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

  public String getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(String fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }
}
