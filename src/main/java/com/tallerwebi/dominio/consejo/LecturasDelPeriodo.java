package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.Diagnostico;
import com.tallerwebi.dominio.asistente.EstadoGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.MesGastoReal;
import com.tallerwebi.dominio.asistente.Veredicto;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Las lecturas del período (HU-16): conclusiones sobre lo que muestra el gráfico, calculadas
 * en Java sobre un GastoReal ya armado. No proponen acciones; se renuevan solas al cerrar el mes.
 */
public class LecturasDelPeriodo {

  // Una categoría tiene que pesar al menos esto del mes para merecer una lectura
  static final double PESO_MINIMO_CATEGORIA = 5.0;
  // Por debajo de esto el mes cerrado se considera igual al anterior
  static final double UMBRAL_BALANCE = 1.0;
  // Meses seguidos en la misma dirección para hablar de racha
  static final int MESES_DE_RACHA = 3;
  // Hormiga sobre presupuesto: alta desde el 5 % (como la alerta amarilla), controlada por debajo del 2 %
  static final double HORMIGA_ALTA = 5.0;
  static final double HORMIGA_CONTROLADA = 2.0;
  // Hace falta al menos esto para hablar de hábitos
  static final int MINIMO_CATEGORIAS_COMPARABLES = 3;
  private static final double DIFERENCIA_MINIMA = 0.5;
  private static final double MEDIO_MES = 0.5;
  private static final double CASI_UN_MES = 0.75;
  private static final double UN_MES_LARGO = 1.5;
  private static final int MINIMO_MESES_CERRADOS = 2;
  private static final String VIVIENDA = "Vivienda";
  private static final String MAS = "más";
  private static final String MENOS = "menos";
  private static final Locale ARGENTINA = Locale.of("es", "AR");

  public List<Consejo> calcular(Usuario usuario, YearMonth mesActual, GastoReal gastoReal) {
    List<Consejo> lecturas = new ArrayList<>();
    agregar(lecturas, veredicto(usuario, mesActual, gastoReal));
    // Hormiga va segunda a propósito: a igual severidad, el tope conserva las primeras
    agregar(lecturas, hormiga(usuario, mesActual, gastoReal));
    agregar(lecturas, balanceDelMes(usuario, mesActual, gastoReal));
    agregar(lecturas, mayorVariacion(usuario, mesActual, gastoReal, Diagnostico.AHORRASTE));
    agregar(lecturas, mayorVariacion(usuario, mesActual, gastoReal, Diagnostico.CONSUMIS_MAS));
    agregar(lecturas, habitos(usuario, mesActual, gastoReal));
    return lecturas;
  }

  private static void agregar(List<Consejo> lecturas, Consejo lectura) {
    if (lectura != null) {
      lecturas.add(lectura);
    }
  }

  // El veredicto del período: cuánto subieron los gastos, cuánto la inflación y la diferencia real
  private Consejo veredicto(Usuario usuario, YearMonth mesActual, GastoReal gastoReal) {
    Veredicto veredicto = gastoReal.getVeredicto();
    if (veredicto == null) {
      return null;
    }
    double real = veredicto.getVariacionReal();
    String cierre;
    if (Math.abs(real) < DIFERENCIA_MINIMA) {
      cierre = "gastás lo mismo en términos reales.";
    } else if (real > 0) {
      cierre = String.format(ARGENTINA, "gastás %.1f %% más en términos reales.", real);
    } else {
      cierre = String.format(ARGENTINA, "gastás %.1f %% menos en términos reales.", -real);
    }
    double nominal = veredicto.getVariacionNominal();
    String movimiento = nominal >= 0
      ? String.format(ARGENTINA, "subieron %.1f %%", nominal)
      : String.format(ARGENTINA, "bajaron %.1f %%", -nominal);
    String texto = String.format(
      ARGENTINA,
      "Desde %s tus gastos %s. La inflación fue %.1f %%: %s",
      nombreDeMes(veredicto.getDesde()),
      movimiento,
      veredicto.getInflacionAcumulada(),
      cierre
    );
    // El veredicto se guarda por período (3, 6 o 12 meses): cada uno tiene su propia síntesis.
    // Va en "categoria" porque es lo que identifica al consejo junto con el tipo
    String periodo = gastoReal.getMeses().size() + " meses";
    String titulo = String.format(
      ARGENTINA,
      "Gastás %s en términos reales que en %s",
      Math.abs(real) < DIFERENCIA_MINIMA
        ? "lo mismo"
        : String.format(ARGENTINA, "%.1f %% %s", Math.abs(real), real > 0 ? MAS : MENOS),
      nombreDeMes(veredicto.getDesde())
    );
    return new Consejo(
      usuario,
      mesActual,
      tipoDelVeredicto(veredicto.getEstado()),
      periodo,
      texto,
      null
    )
      .conTitulo(titulo);
  }

  private static TipoConsejo tipoDelVeredicto(EstadoGastoReal estado) {
    if (estado == EstadoGastoReal.ROJO) {
      return TipoConsejo.VEREDICTO_ROJO;
    }
    return estado == EstadoGastoReal.AMARILLO
      ? TipoConsejo.VEREDICTO_AMARILLO
      : TipoConsejo.VEREDICTO_VERDE;
  }

  // El último mes cerrado contra el anterior, en pesos de hoy; y la racha si viene en la misma dirección
  private Consejo balanceDelMes(Usuario usuario, YearMonth mesActual, GastoReal gastoReal) {
    List<MesGastoReal> cerrados = cerradosConGastos(gastoReal);
    if (cerrados.size() < MINIMO_MESES_CERRADOS) {
      return null;
    }
    MesGastoReal ultimo = cerrados.get(cerrados.size() - 1);
    MesGastoReal anterior = cerrados.get(cerrados.size() - 2);
    double real = variacionReal(anterior, ultimo);
    if (Math.abs(real) < UMBRAL_BALANCE) {
      return null;
    }
    boolean mejor = real < 0;
    StringBuilder texto = new StringBuilder(
      String.format(
        ARGENTINA,
        "%s costó $ %,.0f en pesos de hoy, %.1f %% %s que %s.",
        nombreDeMes(ultimo.getMes()),
        ultimo.getGastadoEnPesosDeHoy(),
        Math.abs(real),
        mejor ? MENOS : MAS,
        nombreDeMes(anterior.getMes())
      )
    );
    int racha = racha(cerrados);
    if (racha >= MESES_DE_RACHA) {
      texto.append(
        String.format(
          ARGENTINA,
          " Es el %d.º mes seguido %s en términos reales.",
          racha,
          mejor ? "bajando" : "subiendo"
        )
      );
    }
    TipoConsejo tipo = mejor ? TipoConsejo.BALANCE_DEL_MES_MEJOR : TipoConsejo.BALANCE_DEL_MES_PEOR;
    String titulo = String.format(
      ARGENTINA,
      "%s costó %.1f %% %s en términos reales",
      nombreDeMes(ultimo.getMes()),
      Math.abs(real),
      mejor ? MENOS : MAS
    );
    return new Consejo(usuario, mesActual, tipo, null, texto.toString(), null)
      .conTitulo(titulo)
      .ancladoAlMes(ultimo.getMes());
  }

  // Cuántos meses seguidos, contando el último, se movieron en la misma dirección que el último
  private static int racha(List<MesGastoReal> cerrados) {
    int cantidad = cerrados.size();
    double signo = Math.signum(
      variacionReal(cerrados.get(cantidad - 2), cerrados.get(cantidad - 1))
    );
    int racha = 1;
    for (int i = cantidad - 2; i > 0; i--) {
      double variacion = variacionReal(cerrados.get(i - 1), cerrados.get(i));
      if (Math.signum(variacion) != signo || Math.abs(variacion) < UMBRAL_BALANCE) {
        break;
      }
      racha++;
    }
    return racha;
  }

  private static double variacionReal(MesGastoReal desde, MesGastoReal hasta) {
    return (hasta.getGastadoEnPesosDeHoy() / desde.getGastadoEnPesosDeHoy() - 1) * 100;
  }

  private static List<MesGastoReal> cerradosConGastos(GastoReal gastoReal) {
    List<MesGastoReal> cerrados = new ArrayList<>();
    for (MesGastoReal mes : gastoReal.getMeses()) {
      if (!mes.isEnCurso() && mes.getGastado() > 0) {
        cerrados.add(mes);
      }
    }
    return cerrados;
  }

  // La categoría que más bajó (AHORRASTE) o más subió (CONSUMIS_MAS) en términos reales, si pesa en el mes
  private Consejo mayorVariacion(
    Usuario usuario,
    YearMonth mesActual,
    GastoReal gastoReal,
    Diagnostico buscado
  ) {
    CategoriaGastoReal elegida = laQueMasVario(gastoReal.getCategorias(), buscado);
    if (elegida == null) {
      return null;
    }
    boolean baja = buscado == Diagnostico.AHORRASTE;
    YearMonth ultimo = gastoReal.getUltimoMesCerrado();
    String texto = String.format(
      ARGENTINA,
      "%s fue lo que más %s en %s descontada la inflación: %.1f %% %s que en %s ($ %,.0f contra $ %,.0f).",
      elegida.getNombre(),
      baja ? "bajó" : "subió",
      nombreDeMes(ultimo),
      Math.abs(elegida.getVariacionReal()),
      baja ? MENOS : MAS,
      nombreDeMes(ultimo.minusMonths(1)),
      elegida.getGastado(),
      elegida.getGastadoAnterior()
    );
    TipoConsejo tipo = baja ? TipoConsejo.MAYOR_BAJA_REAL : TipoConsejo.MAYOR_SUBA_REAL;
    String titulo = String.format(
      ARGENTINA,
      "%s %s %.1f %% real",
      elegida.getNombre(),
      baja ? "bajó" : "subió",
      Math.abs(elegida.getVariacionReal())
    );
    return new Consejo(usuario, mesActual, tipo, elegida.getNombre(), texto, null)
      .conTitulo(titulo)
      .ancladoALaCategoria(elegida.getNombre());
  }

  // Entre las categorías con ese diagnóstico que pesan en el mes, la de mayor variación real
  private static CategoriaGastoReal laQueMasVario(
    List<CategoriaGastoReal> categorias,
    Diagnostico buscado
  ) {
    double total = categorias.stream().mapToDouble(CategoriaGastoReal::getGastado).sum();
    CategoriaGastoReal elegida = null;
    for (CategoriaGastoReal categoria : categorias) {
      boolean pesa = categoria.getGastado() * 100 >= total * PESO_MINIMO_CATEGORIA;
      if (categoria.getDiagnostico() != buscado || !pesa) {
        continue;
      }
      if (
        elegida == null ||
        Math.abs(categoria.getVariacionReal()) > Math.abs(elegida.getVariacionReal())
      ) {
        elegida = categoria;
      }
    }
    return elegida;
  }

  // Si en la mayoría de las categorías el cambio fue solo el precio, no hubo cambio de hábitos
  private Consejo habitos(Usuario usuario, YearMonth mesActual, GastoReal gastoReal) {
    int comparables = 0;
    int conLaInflacion = 0;
    for (CategoriaGastoReal categoria : gastoReal.getCategorias()) {
      Diagnostico diagnostico = categoria.getDiagnostico();
      if (diagnostico == Diagnostico.NUEVO || diagnostico == Diagnostico.SIN_COMPARACION) {
        continue;
      }
      comparables++;
      if (diagnostico == Diagnostico.SUBIO_EL_PRECIO || diagnostico == Diagnostico.ESTABLE) {
        conLaInflacion++;
      }
    }
    if (comparables < MINIMO_CATEGORIAS_COMPARABLES || conLaInflacion * 2 < comparables) {
      return null;
    }
    YearMonth ultimo = gastoReal.getUltimoMesCerrado();
    String texto = String.format(
      ARGENTINA,
      "En %d de %d categorías gastaste en %s lo mismo que en %s en términos reales: lo que cambió fue el precio, no tus hábitos.",
      conLaInflacion,
      comparables,
      nombreDeMes(ultimo),
      nombreDeMes(ultimo.minusMonths(1))
    );
    String titulo = String.format(
      ARGENTINA,
      "%d de %d categorías: solo subió el precio",
      conLaInflacion,
      comparables
    );
    return new Consejo(usuario, mesActual, TipoConsejo.HABITOS_CON_LA_INFLACION, null, texto, null)
      .conTitulo(titulo);
  }

  // Los gastos hormiga de los meses cerrados: cuánto fueron, qué parte del margen se llevaron y a qué
  // equivalen. El mes en curso no entra: eso lo muestra Resumen (reglas del asistente, 1)
  private Consejo hormiga(Usuario usuario, YearMonth mesActual, GastoReal gastoReal) {
    AcumuladoHormiga acumulado = acumularHormiga(gastoReal);
    if (acumulado == null) {
      return null;
    }
    double porcentaje = (acumulado.total() / acumulado.presupuesto()) * 100;
    String periodo = nombreDeMes(acumulado.desde()) + " y " + nombreDeMes(acumulado.hasta());
    if (acumulado.total() <= 0) {
      String texto = String.format(
        ARGENTINA,
        "No tuviste gastos hormiga entre %s: lo chico y prescindible está bajo control.",
        periodo
      );
      return new Consejo(usuario, mesActual, TipoConsejo.HORMIGA_CONTROLADA, null, texto, null)
        .conTitulo("Sin gastos hormiga en el período");
    }
    if (porcentaje < HORMIGA_ALTA) {
      String texto = porcentaje < HORMIGA_CONTROLADA
        ? String.format(
          ARGENTINA,
          "Tus gastos hormiga están bajo control: $ %,.0f entre %s, el %.1f %% del presupuesto.",
          acumulado.total(),
          periodo,
          porcentaje
        )
        : String.format(
          ARGENTINA,
          "Entre %s se te fueron $ %,.0f en gastos hormiga, el %.1f %% del presupuesto.",
          periodo,
          acumulado.total(),
          porcentaje
        );
      return new Consejo(usuario, mesActual, TipoConsejo.HORMIGA_CONTROLADA, null, texto, null)
        .conTitulo(
          String.format(ARGENTINA, "Gastos hormiga: $ %,.0f en el período", acumulado.total())
        )
        .ancladoAlMes(acumulado.pico().getMes());
    }
    // Una idea y tres números: el total, qué parte del margen se llevó y el pico
    StringBuilder texto = new StringBuilder(
      String.format(
        ARGENTINA,
        "Entre %s se te fueron $ %,.0f en gastos hormiga",
        periodo,
        acumulado.total()
      )
    );
    if (acumulado.margen() > 0) {
      texto.append(
        String.format(
          ARGENTINA,
          ": el %.0f %% de lo que te sobró",
          Math.min((acumulado.total() / acumulado.margen()) * 100, 100)
        )
      );
    }
    texto.append(
      String.format(
        ARGENTINA,
        ", con el pico en %s ($ %,.0f).",
        nombreDeMes(acumulado.pico().getMes()),
        acumulado.pico().getHormiga()
      )
    );
    texto.append(equivalencia(gastoReal, acumulado.total() / 2));
    return new Consejo(usuario, mesActual, TipoConsejo.HORMIGA_ALTA, null, texto.toString(), null)
      .conTitulo(
        String.format(ARGENTINA, "Gastos hormiga: $ %,.0f en el período", acumulado.total())
      )
      .ancladoAlMes(acumulado.pico().getMes());
  }

  private record AcumuladoHormiga(
    double total,
    double presupuesto,
    double margen,
    MesGastoReal pico,
    YearMonth desde,
    YearMonth hasta
  ) {}

  // Meses cerrados con presupuesto: sin presupuesto no hay con qué medir lo "chico"
  private static AcumuladoHormiga acumularHormiga(GastoReal gastoReal) {
    List<MesGastoReal> meses = new ArrayList<>();
    for (MesGastoReal mes : gastoReal.getMeses()) {
      if (cuentaParaHormiga(mes)) {
        meses.add(mes);
      }
    }
    if (meses.size() < MINIMO_MESES_CERRADOS) {
      return null;
    }
    double total = 0;
    double presupuesto = 0;
    double margen = 0;
    MesGastoReal pico = meses.get(0);
    for (MesGastoReal mes : meses) {
      total += mes.getHormiga();
      presupuesto += mes.getPresupuesto();
      margen += Math.max(mes.getPresupuesto() - mes.getGastado(), 0);
      pico = mes.getHormiga() > pico.getHormiga() ? mes : pico;
    }
    YearMonth desde = meses.get(0).getMes();
    YearMonth hasta = meses.get(meses.size() - 1).getMes();
    return new AcumuladoHormiga(total, presupuesto, margen, pico, desde, hasta);
  }

  private static boolean cuentaParaHormiga(MesGastoReal mes) {
    return (
      !mes.isEnCurso() &&
      mes.getPresupuesto() != null &&
      mes.getPresupuesto() > 0 &&
      mes.getHormiga() != null
    );
  }

  // "Con la mitad pagabas casi un mes de Alimentación": la categoría principal del último mes cerrado, sin Vivienda
  private static String equivalencia(GastoReal gastoReal, double monto) {
    CategoriaGastoReal principal = null;
    for (CategoriaGastoReal categoria : gastoReal.getCategorias()) {
      if (VIVIENDA.equals(categoria.getNombre())) {
        continue;
      }
      if (principal == null || categoria.getGastado() > principal.getGastado()) {
        principal = categoria;
      }
    }
    if (principal == null || principal.getGastado() <= 0) {
      return "";
    }
    String cuanto = enMeses(monto / principal.getGastado());
    return cuanto.isEmpty()
      ? ""
      : " Con la mitad pagabas " + cuanto + " de " + principal.getNombre() + ".";
  }

  // En palabras, no en decimales: menos de medio mes no se dice
  public static String enMeses(double meses) {
    if (meses < MEDIO_MES) {
      return "";
    }
    if (meses < CASI_UN_MES) {
      return "más de medio mes";
    }
    if (meses < UN_MES_LARGO) {
      return meses < 1 ? "casi un mes" : "un mes";
    }
    return String.format(ARGENTINA, "%d meses", Math.round(meses));
  }

  static String nombreDeMes(YearMonth mes) {
    String nombre = mes.getMonth().getDisplayName(TextStyle.FULL, ARGENTINA);
    return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + mes.getYear();
  }
}
