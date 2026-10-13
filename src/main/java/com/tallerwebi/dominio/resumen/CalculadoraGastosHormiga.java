package com.tallerwebi.dominio.resumen;

import com.tallerwebi.dominio.gasto.Gasto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un gasto del mes es hormiga si pasa tres filtros, en este orden: su categoría es prescindible,
 * es chico (importe hasta el 2 % del presupuesto) y es frecuente (su categoría tiene 4 o más gastos
 * chicos en el mes)
 */
public class CalculadoraGastosHormiga {

  static final double PORCENTAJE_GASTO_CHICO = 2.0;
  static final int MINIMO_GASTOS_FRECUENTE = 4;

  public ResumenHormiga calcular(
    List<Gasto> gastosDelMes,
    double presupuesto,
    YearMonth mes,
    LocalDate hoy
  ) {
    double importeMaximoChico = (presupuesto * PORCENTAJE_GASTO_CHICO) / 100;

    Map<String, List<Gasto>> chicosPorCategoria = new LinkedHashMap<>();
    for (Gasto gasto : gastosDelMes) {
      if (esPrescindible(gasto) && gasto.getImporte() <= importeMaximoChico) {
        chicosPorCategoria
          .computeIfAbsent(gasto.getCategoria().getNombre(), nombre -> new ArrayList<>())
          .add(gasto);
      }
    }

    double total = 0.0;
    Map<String, Double> totalPorCategoria = new LinkedHashMap<>();
    for (Map.Entry<String, List<Gasto>> categoria : chicosPorCategoria.entrySet()) {
      if (categoria.getValue().size() >= MINIMO_GASTOS_FRECUENTE) {
        double totalCategoria = categoria.getValue().stream().mapToDouble(Gasto::getImporte).sum();
        totalPorCategoria.put(categoria.getKey(), totalCategoria);
        total += totalCategoria;
      }
    }

    List<ResumenCategoria> categorias = new ArrayList<>();
    for (Map.Entry<String, Double> categoria : totalPorCategoria.entrySet()) {
      categorias.add(
        new ResumenCategoria(
          categoria.getKey(),
          categoria.getValue(),
          porcentaje(categoria.getValue(), total)
        )
      );
    }
    categorias.sort(Comparator.comparing(ResumenCategoria::getGastado).reversed());

    double proyeccion = proyectar(total, mes, hoy);

    return new ResumenHormiga(
      total,
      porcentaje(total, presupuesto),
      proyeccion,
      porcentaje(proyeccion, presupuesto),
      categorias
    );
  }

  // Un gasto sin categoría se trata como esencial
  private boolean esPrescindible(Gasto gasto) {
    return gasto.getCategoria() != null && !gasto.getCategoria().isEsencial();
  }

  // Solo el mes en curso se proyecta: uno ya cerrado (o futuro) termina con lo que tiene
  private double proyectar(double total, YearMonth mes, LocalDate hoy) {
    if (!mes.equals(YearMonth.from(hoy))) {
      return total;
    }
    return (total / hoy.getDayOfMonth()) * mes.lengthOfMonth();
  }

  private double porcentaje(double parte, double total) {
    return total > 0 ? (parte / total) * 100 : 0.0;
  }
}
