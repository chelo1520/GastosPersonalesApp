package com.tallerwebi.dominio.resumen;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.MarcasHormiga;
import com.tallerwebi.dominio.gasto.Gasto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un gasto del mes es hormiga si pasa tres filtros, en este orden: su categoría es prescindible
 * para el usuario (sus marcas, o lo que dice la categoría si no la marcó), es chico (importe hasta
 * el 2 % del presupuesto) y es frecuente (su categoría tiene 4 o más gastos chicos en el mes)
 */
public class CalculadoraGastosHormiga {

  static final double PORCENTAJE_GASTO_CHICO = 2.0;
  static final int MINIMO_GASTOS_FRECUENTE = 4;

  /** Con las marcas del maestro de categorías, sin preferencias del usuario. */
  public ResumenHormiga calcular(List<Gasto> gastosDelMes, double presupuesto) {
    return calcular(gastosDelMes, presupuesto, MarcasHormiga.porDefecto());
  }

  public ResumenHormiga calcular(
    List<Gasto> gastosDelMes,
    double presupuesto,
    MarcasHormiga marcas
  ) {
    Map<String, List<Gasto>> chicosPorCategoria = new LinkedHashMap<>();
    for (Gasto gasto : gastosDelMes) {
      if (esChicoYPrescindible(gasto, presupuesto, marcas)) {
        chicosPorCategoria
          .computeIfAbsent(gasto.getCategoria().getNombre(), nombre -> new ArrayList<>())
          .add(gasto);
      }
    }

    List<List<Gasto>> frecuentes = chicosPorCategoria
      .values()
      .stream()
      .filter(gastos -> gastos.size() >= MINIMO_GASTOS_FRECUENTE)
      .toList();
    double total = frecuentes.stream().mapToDouble(CalculadoraGastosHormiga::sumar).sum();

    List<ResumenCategoria> categorias = new ArrayList<>();
    for (List<Gasto> gastos : frecuentes) {
      double totalCategoria = sumar(gastos);
      Categoria categoria = gastos.get(0).getCategoria();
      categorias.add(
        new ResumenCategoria(
          categoria.getNombre(),
          totalCategoria,
          porcentaje(totalCategoria, total),
          null,
          categoria.getId()
        )
      );
    }
    categorias.sort(Comparator.comparing(ResumenCategoria::getGastado).reversed());

    return new ResumenHormiga(total, porcentaje(total, presupuesto), categorias);
  }

  /** Con las marcas del maestro de categorías, sin preferencias del usuario. */
  public static boolean esChicoYPrescindible(Gasto gasto, double presupuesto) {
    return esChicoYPrescindible(gasto, presupuesto, MarcasHormiga.porDefecto());
  }

  // Un gasto sin categoría se trata como esencial (lo resuelven las marcas)
  public static boolean esChicoYPrescindible(
    Gasto gasto,
    double presupuesto,
    MarcasHormiga marcas
  ) {
    double importeMaximoChico = (presupuesto * PORCENTAJE_GASTO_CHICO) / 100;
    return marcas.esPrescindible(gasto.getCategoria()) && gasto.getImporte() <= importeMaximoChico;
  }

  private static double sumar(List<Gasto> gastos) {
    return gastos.stream().mapToDouble(Gasto::getImporte).sum();
  }

  private double porcentaje(double parte, double total) {
    return total > 0 ? (parte / total) * 100 : 0.0;
  }
}
