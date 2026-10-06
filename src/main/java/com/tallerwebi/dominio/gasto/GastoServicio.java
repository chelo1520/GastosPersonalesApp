package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * GastoServicio
 */
public interface GastoServicio {
  void registrarGasto(Gasto gasto, Usuario usuario);

  List<Gasto> obtenerGastos(Usuario usuario);

  Double sumarGastos(Usuario usuario, LocalDate desde, LocalDate hasta);

  List<Gasto> obtenerGastosDelMesAnterior(Usuario usuario, YearMonth mesActual);
}
