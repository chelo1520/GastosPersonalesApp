package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import java.time.LocalDate;
import java.util.List;

/**
 * RepositorioGasto
 */
public interface RepositorioGasto {
  void guardar(Gasto gasto);

  List<Gasto> BuscarGastosPorUsuario(Usuario usuario);

  List<Gasto> obtenerGastosEntreFechas(Usuario usuario, LocalDate desde, LocalDate hasta);
}
