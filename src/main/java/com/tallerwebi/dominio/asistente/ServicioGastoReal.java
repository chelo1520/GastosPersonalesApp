package com.tallerwebi.dominio.asistente;

import com.tallerwebi.dominio.Usuario;

/**
 * ServicioGastoReal: cruza el historial de gastos con el IPC y el dólar
 */
@FunctionalInterface
public interface ServicioGastoReal {
  int PERIODO_POR_DEFECTO = 6;

  // Los últimos cantidadMeses meses, terminando en el mes en curso
  GastoReal calcular(Usuario usuario, int cantidadMeses);
}
