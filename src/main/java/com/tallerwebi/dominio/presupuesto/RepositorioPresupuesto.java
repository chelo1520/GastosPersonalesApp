package com.tallerwebi.dominio.presupuesto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import java.time.YearMonth;
import java.util.List;

public interface RepositorioPresupuesto {
  void guardar(Presupuesto presupuesto);

  void modificar(Presupuesto presupuesto);

  // null si el usuario no definió presupuesto para esa categoría en ese mes
  Presupuesto buscar(Usuario usuario, Categoria categoria, YearMonth mes);

  List<Presupuesto> obtenerDelMes(Usuario usuario, YearMonth mes);

  List<Presupuesto> obtenerDelUsuario(Usuario usuario);

  // Crea o reemplaza el presupuesto total de ese mes
  void guardarTotal(PresupuestoTotal total);

  // null si el usuario no definió un total para ese mes
  PresupuestoTotal buscarTotal(Usuario usuario, YearMonth mes);
}
