package com.tallerwebi.dominio.asistente;

import com.tallerwebi.dominio.Usuario;
import java.time.YearMonth;
import java.util.List;

/**
 * ServicioGastoReal: cruza el historial de gastos con el IPC y el dólar
 */
public interface ServicioGastoReal {
  int PERIODO_POR_DEFECTO = 6;

  // Los últimos cantidadMeses meses, terminando en el mes en curso
  GastoReal calcular(Usuario usuario, int cantidadMeses);

  // Cada categoría de un mes contra el mes anterior, descontando el IPC del mes; las que más
  // subieron en términos reales primero
  List<CategoriaGastoReal> categoriasDelMes(Usuario usuario, YearMonth mes);
}
