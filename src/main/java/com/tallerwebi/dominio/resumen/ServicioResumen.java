package com.tallerwebi.dominio.resumen;

import com.tallerwebi.dominio.Usuario;
import java.time.YearMonth;
import java.util.List;

/**
 * ServicioResumen
 */
public interface ServicioResumen {
  ResumenMensual obtenerResumen(Usuario usuario, YearMonth mes);

  // Los últimos cantidadMeses meses que terminan en hasta (incluido), del más viejo al más nuevo.
  // Los meses sin gastos vienen en cero
  List<ResumenDeMes> obtenerHistorial(Usuario usuario, YearMonth hasta, int cantidadMeses);
}
