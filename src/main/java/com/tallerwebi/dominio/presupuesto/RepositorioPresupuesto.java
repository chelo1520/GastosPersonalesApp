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
}
