package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.alerta.Alerta;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * GastoServicio
 */
public interface GastoServicio {
  void registrarGasto(Gasto gasto, Usuario usuario);

  // Los gastos de un comprobante: se guardan todos o ninguno (si uno es inválido, no se guarda nada)
  void registrarGastos(List<Gasto> gastos, Usuario usuario);

  // Alerta de repetición para un gasto recién registrado, o null si no corresponde
  Alerta alertaDeRepeticion(Gasto gasto, Usuario usuario);

  // Alerta si el gasto recién registrado hizo cruzar el 80 % o el 100 % del presupuesto del mes,
  // o null si no corresponde
  Alerta alertaDePresupuesto(Gasto gasto, Usuario usuario);

  // Igual, pero para varios gastos recién registrados juntos: avisa si entre todos cruzaron el umbral
  Alerta alertaDePresupuesto(List<Gasto> gastos, Usuario usuario);

  Gasto obtenerGasto(Long id, Usuario usuario);

  void modificarGasto(Long id, Gasto datos, Usuario usuario);

  void eliminarGasto(Long id, Usuario usuario);

  List<Gasto> obtenerGastos(Usuario usuario);

  Double sumarGastos(Usuario usuario, LocalDate desde, LocalDate hasta);

  List<Gasto> obtenerGastosDelMesAnterior(Usuario usuario, YearMonth mesActual);

  LocalDate obtenerFechaMinimaPermitida();

  LocalDate obtenerFechaMaximaPermitida();
}
