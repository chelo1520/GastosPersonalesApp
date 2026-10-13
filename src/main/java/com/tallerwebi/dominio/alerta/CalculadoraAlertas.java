package com.tallerwebi.dominio.alerta;

import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.resumen.CalculadoraGastosHormiga;
import com.tallerwebi.dominio.resumen.ResumenHormiga;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Las tres alertas de gastos hormiga. Se calculan cada vez que hacen falta: no se guardan
 */
public class CalculadoraAlertas {

  static final double PORCENTAJE_ALERTA_AMARILLA = 5.0;
  static final double PORCENTAJE_ALERTA_ROJA = 10.0;
  static final int GASTOS_PARA_ALERTA_REPETICION = 4;
  public static final int DIAS_ALERTA_REPETICION = 7;

  private static final Locale ARGENTINA = Locale.of("es", "AR");

  // Acumulado: amarilla desde el 5 %, roja desde el 10 %
  public List<Alerta> alertasDelMes(ResumenHormiga hormiga) {
    List<Alerta> alertas = new ArrayList<>();
    double porcentaje = hormiga.getPorcentajePresupuesto();

    if (porcentaje >= PORCENTAJE_ALERTA_AMARILLA) {
      Alerta.Nivel nivel = porcentaje >= PORCENTAJE_ALERTA_ROJA
        ? Alerta.Nivel.ROJA
        : Alerta.Nivel.AMARILLA;
      alertas.add(
        new Alerta(
          Alerta.Tipo.ACUMULADO,
          nivel,
          "Llevás " +
          pesos(hormiga.getTotal()) +
          " en gastos hormiga, el " +
          Math.round(porcentaje) +
          "% de tu presupuesto",
          null
        )
      );
    }

    return alertas;
  }

  // gastosDeLaSemana: los gastos del usuario en los 7 días que terminan en la fecha del gasto nuevo,
  // incluido el gasto nuevo. Devuelve null si no corresponde alertar
  public Alerta alertaDeRepeticion(Gasto nuevo, List<Gasto> gastosDeLaSemana, double presupuesto) {
    if (!CalculadoraGastosHormiga.esChicoYPrescindible(nuevo, presupuesto)) {
      return null;
    }

    List<Gasto> repetidos = gastosDeLaSemana
      .stream()
      .filter(gasto -> CalculadoraGastosHormiga.esChicoYPrescindible(gasto, presupuesto))
      .filter(gasto -> Objects.equals(gasto.getCategoria().getId(), nuevo.getCategoria().getId()))
      .toList();

    if (repetidos.size() < GASTOS_PARA_ALERTA_REPETICION) {
      return null;
    }

    double total = repetidos.stream().mapToDouble(Gasto::getImporte).sum();
    return new Alerta(
      Alerta.Tipo.REPETICION,
      Alerta.Nivel.AMARILLA,
      "Es tu " +
      repetidos.size() +
      ".º gasto en " +
      nuevo.getCategoria().getNombre() +
      " esta semana (" +
      pesos(total) +
      ")",
      nuevo.getCategoria().getId()
    );
  }

  private String pesos(double importe) {
    return "$" + NumberFormat.getIntegerInstance(ARGENTINA).format(Math.round(importe));
  }
}
