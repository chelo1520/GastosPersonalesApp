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

  void definirPorcentaje(Usuario usuario, Categoria categoria, YearMonth mes, Double porcentaje);

  Presupuesto buscar(Usuario usuario, Categoria categoria, YearMonth mes);

  List<Presupuesto> obtenerDelMes(Usuario usuario, YearMonth mes);

  // Suma de los presupuestos por categoría del mes; 0 si no hay ninguno definido
  Double totalDelMes(Usuario usuario, YearMonth mes);

  // Total del mes: el definido para ese mes o, si no hay, el del perfil (ingreso x porcentaje); 0 si no hay ninguno
  Double presupuestoDelMes(Usuario usuario, YearMonth mes);

  // Define el total de un mes; no puede ser menor a lo ya asignado a categorías
  void definirPresupuestoDelMes(Usuario usuario, YearMonth mes, Double monto);

  // El presupuesto por defecto rige los meses sin total propio: no puede quedar por debajo de lo ya asignado en ellos
  void validarPresupuestoPorDefecto(Usuario usuario, Double nuevoPorDefecto);

  // Presupuesto del mes menos la suma por categoría; negativo si las categorías lo superan
  Double noAsignadoDelMes(Usuario usuario, YearMonth mes);

  // Los doce meses de un año con su total (propio o del perfil) y lo asignado a categorías
  List<MesDePresupuesto> resumenDelAnio(Usuario usuario, int anio);
}
