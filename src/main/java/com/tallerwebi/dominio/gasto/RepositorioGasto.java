package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * RepositorioGasto
 */
public interface RepositorioGasto {
  void guardar(Gasto gasto);
  void modificar(Gasto gasto);
  void eliminar(Gasto gasto);
  Gasto buscarPorId(Long id);
  List<Gasto> buscarGastosPorUsuario(Usuario usuario);
  List<Gasto> obtenerGastosEntreFechas(Usuario usuario, LocalDate desde, LocalDate hasta);
  List<Gasto> obtenerGastosDelMes(Usuario usuario, YearMonth mes);

  // Los más recientes del usuario, del más nuevo al más viejo (fecha y, a igual fecha, alta)
  List<Gasto> obtenerUltimos(Usuario usuario, int cantidad);
}
