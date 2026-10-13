package com.tallerwebi.dominio.presupuesto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import java.time.YearMonth;
import java.util.List;

/**
 * ServicioPresupuesto
 */
public interface ServicioPresupuesto {
  // Uno por categoría y mes: si ya existe, reemplaza el monto
  void definir(Usuario usuario, Categoria categoria, YearMonth mes, Double monto);

  Presupuesto buscar(Usuario usuario, Categoria categoria, YearMonth mes);

  List<Presupuesto> obtenerDelMes(Usuario usuario, YearMonth mes);

  // Suma de los presupuestos por categoría del mes; 0 si no hay ninguno definido
  Double totalDelMes(Usuario usuario, YearMonth mes);
}
