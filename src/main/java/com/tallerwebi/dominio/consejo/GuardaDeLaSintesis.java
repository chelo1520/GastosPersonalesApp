package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.asistente.Veredicto;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Guarda de la síntesis: el párrafo que escribe la IA reemplaza en pantalla a la oración del
 * veredicto, así que tiene que decir los mismos números que calculó Java (subida de los gastos,
 * inflación y diferencia real). Si falta alguno o cambió, la síntesis se descarta y queda la regla.
 */
public final class GuardaDeLaSintesis {

  // Por debajo de esto el veredicto dice "lo mismo" y no exige el número de la diferencia real
  static final double DIFERENCIA_MINIMA = 0.5;
  private static final Locale ARGENTINA = Locale.of("es", "AR");

  private GuardaDeLaSintesis() {}

  public static boolean respetaElVeredicto(String sintesis, Veredicto veredicto) {
    if (sintesis == null || veredicto == null) {
      return false;
    }
    boolean nominal = menciona(sintesis, Math.abs(veredicto.getVariacionNominal()));
    boolean inflacion = menciona(sintesis, veredicto.getInflacionAcumulada());
    double real = veredicto.getVariacionReal();
    boolean diferencia = Math.abs(real) < DIFERENCIA_MINIMA
      ? sintesis.toLowerCase(ARGENTINA).contains("lo mismo") || menciona(sintesis, Math.abs(real))
      : menciona(sintesis, Math.abs(real));
    return nominal && inflacion && diferencia;
  }

  // "9,4" tal como lo formatea la regla; "30,0" también vale como "30". Sin que sea parte de otro número
  static boolean menciona(String texto, double valor) {
    String numero = String.format(ARGENTINA, "%.1f", valor);
    String forma = numero.endsWith(",0")
      ? Pattern.quote(numero.substring(0, numero.length() - 2)) + "(,0)?"
      : Pattern.quote(numero);
    return Pattern.compile("(?<![\\d,.])" + forma + "(?![\\d])").matcher(texto).find();
  }
}
