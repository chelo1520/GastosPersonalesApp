package com.tallerwebi.dominio;

/**
 * ServicioPerfil
 */
@FunctionalInterface
public interface ServicioPerfil {
  // Ingreso mensual y porcentaje (0-100] que el usuario destina a su presupuesto
  void definirPresupuesto(Usuario usuario, Double ingresoMensual, Double porcentaje);
}
