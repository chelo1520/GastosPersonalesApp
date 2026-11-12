package com.tallerwebi.dominio;

import java.time.LocalDate;

/**
 * ServicioPerfil
 */
public interface ServicioPerfil {
  // Ingreso mensual y porcentaje (0-100] que el usuario destina a su presupuesto
  void definirPresupuesto(Usuario usuario, Double ingresoMensual, Double porcentaje);

  // Nombre, apellido y fecha de nacimiento: opcionales; vacío borra el dato
  void actualizarDatosPersonales(
    Usuario usuario,
    String nombre,
    String apellido,
    LocalDate fechaNacimiento
  );
}
