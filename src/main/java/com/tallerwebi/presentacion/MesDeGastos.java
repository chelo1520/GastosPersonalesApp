package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.gasto.Gasto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Los gastos de un mes armados para la vista "Mis gastos" del rediseño v2: el mapa de calor (una
 * casilla por día, más oscura cuanto más se gastó), la lista agrupada por día, la lista por importe
 * y el total por categoría. El filtrado (por día, categoría o texto) lo hace el navegador sobre esto.
 */
public class MesDeGastos {

  /** Niveles del mapa de calor: 0 es un día sin gastos; de 1 a 4, del día que menos se gastó al que más. */
  static final int NIVEL_MAXIMO = 4;

  private static final Locale ARGENTINA = Locale.of("es", "AR");
  private static final DateTimeFormatter DIA_Y_MES = DateTimeFormatter.ofPattern(
    "d 'de' MMMM",
    ARGENTINA
  );
  private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern(
    "EEEE d 'de' MMMM",
    ARGENTINA
  );
  private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern(
    "d MMM",
    ARGENTINA
  );

  private final List<Gasto> gastos;
  private final YearMonth mes;
  private final LocalDate hoy;

  public MesDeGastos(List<Gasto> gastos, YearMonth mes, LocalDate hoy) {
    this.gastos =
      gastos
        .stream()
        .filter(g -> YearMonth.from(g.getFecha()).equals(mes))
        .sorted(
          Comparator
            .comparing(Gasto::getFecha, Comparator.reverseOrder())
            .thenComparing(Gasto::getId, Comparator.nullsLast(Comparator.reverseOrder()))
        )
        .toList();
    this.mes = mes;
    this.hoy = hoy;
  }

  public double getTotal() {
    return gastos.stream().mapToDouble(Gasto::getImporte).sum();
  }

  public int getCantidad() {
    return gastos.size();
  }

  public boolean isVacio() {
    return gastos.isEmpty();
  }

  /** Una casilla por día del mes, del 1 al último. */
  public List<Dia> getDias() {
    Map<Integer, Double> porDia = totalesPorDia();
    List<Double> montos = porDia.values().stream().distinct().sorted().toList();
    List<Dia> dias = new ArrayList<>();
    for (int numero = 1; numero <= mes.lengthOfMonth(); numero++) {
      double total = porDia.getOrDefault(numero, 0.0);
      boolean futuro = mes.atDay(numero).isAfter(hoy);
      dias.add(new Dia(numero, total, nivel(total, montos), futuro, mes));
    }
    return dias;
  }

  /** Grupos por día, del más reciente al más viejo; dentro del día, el último cargado primero. */
  public List<Grupo> getGrupos() {
    Map<LocalDate, List<Gasto>> porFecha = new LinkedHashMap<>();
    for (Gasto gasto : gastos) {
      porFecha.computeIfAbsent(gasto.getFecha(), f -> new ArrayList<>()).add(gasto);
    }
    List<Grupo> grupos = new ArrayList<>();
    porFecha.forEach((fecha, delDia) ->
      grupos.add(new Grupo(fecha, tituloDelDia(fecha), filas(delDia)))
    );
    return grupos;
  }

  /** Todos los gastos del mes de mayor a menor importe. */
  public List<Fila> getPorImporte() {
    return filas(
      gastos.stream().sorted(Comparator.comparing(Gasto::getImporte).reversed()).toList()
    );
  }

  /** Total por categoría, de la que más gastó a la que menos. */
  public List<TotalDeCategoria> getCategorias() {
    Map<String, Double> porCategoria = new TreeMap<>();
    for (Gasto gasto : gastos) {
      porCategoria.merge(nombreDeCategoria(gasto), gasto.getImporte(), Double::sum);
    }
    double maximo = porCategoria.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
    return porCategoria
      .entrySet()
      .stream()
      .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
      .map(e -> new TotalDeCategoria(e.getKey(), e.getValue(), maximo))
      .toList();
  }

  // El nivel sale del puesto del día entre los días con gasto (montos: sin repetir, de menor a mayor),
  // no de su proporción con el máximo: así un día con el alquiler no deja a todos los demás en blanco.
  static int nivel(double total, List<Double> montos) {
    if (total <= 0 || montos.isEmpty()) {
      return 0;
    }
    // Con un solo día con gasto, ese día es el de más gasto
    int ultimoPuesto = montos.size() - 1;
    if (ultimoPuesto == 0) {
      return NIVEL_MAXIMO;
    }
    int puesto = montos.indexOf(total);
    return 1 + (int) Math.round(((double) (NIVEL_MAXIMO - 1) * puesto) / ultimoPuesto);
  }

  private Map<Integer, Double> totalesPorDia() {
    Map<Integer, Double> porDia = new TreeMap<>();
    for (Gasto gasto : gastos) {
      porDia.merge(gasto.getFecha().getDayOfMonth(), gasto.getImporte(), Double::sum);
    }
    return porDia;
  }

  private String tituloDelDia(LocalDate fecha) {
    if (fecha.equals(hoy)) {
      return "Hoy · " + fecha.format(DIA_Y_MES);
    }
    if (fecha.equals(hoy.minusDays(1))) {
      return "Ayer · " + fecha.format(DIA_Y_MES);
    }
    return conMayuscula(fecha.format(DIA_SEMANA));
  }

  private List<Fila> filas(List<Gasto> lista) {
    return lista.stream().map(Fila::new).toList();
  }

  static String nombreDeCategoria(Gasto gasto) {
    return gasto.getCategoria() == null ? "Sin categoría" : gasto.getCategoria().getNombre();
  }

  private static String conMayuscula(String texto) {
    return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
  }

  static String pesos(double importe) {
    return String.format(ARGENTINA, "$ %,.2f", importe);
  }

  /** Una casilla del mapa de calor. */
  public static class Dia {

    private final int numero;
    private final double total;
    private final int nivel;
    private final boolean futuro;
    private final YearMonth mes;

    Dia(int numero, double total, int nivel, boolean futuro, YearMonth mes) {
      this.numero = numero;
      this.total = total;
      this.nivel = nivel;
      this.futuro = futuro;
      this.mes = mes;
    }

    public int getNumero() {
      return numero;
    }

    public double getTotal() {
      return total;
    }

    public int getNivel() {
      return nivel;
    }

    public boolean isFuturo() {
      return futuro;
    }

    // "12/11 · $ 4.000,00": el texto que aparece al pasar el mouse
    public String getDescripcion() {
      return numero + "/" + mes.getMonthValue() + " · " + pesos(total);
    }
  }

  /** Los gastos de un día, con su título ("Hoy · 12 de noviembre") y su total. */
  public static class Grupo {

    private final LocalDate fecha;
    private final String titulo;
    private final List<Fila> filas;

    Grupo(LocalDate fecha, String titulo, List<Fila> filas) {
      this.fecha = fecha;
      this.titulo = titulo;
      this.filas = filas;
    }

    public LocalDate getFecha() {
      return fecha;
    }

    public int getDia() {
      return fecha.getDayOfMonth();
    }

    public String getTitulo() {
      return titulo;
    }

    public List<Fila> getFilas() {
      return filas;
    }

    public double getTotal() {
      return filas.stream().mapToDouble(Fila::getImporte).sum();
    }
  }

  /** Un gasto en la lista: avatar con la inicial de la descripción y el color de su categoría. */
  public static class Fila {

    private final Gasto gasto;

    Fila(Gasto gasto) {
      this.gasto = gasto;
    }

    public Long getId() {
      return gasto.getId();
    }

    public String getDescripcion() {
      return gasto.getDescripcion();
    }

    public double getImporte() {
      return gasto.getImporte();
    }

    public String getCategoria() {
      return nombreDeCategoria(gasto);
    }

    public int getDia() {
      return gasto.getFecha().getDayOfMonth();
    }

    public LocalDate getFecha() {
      return gasto.getFecha();
    }

    // "10 nov": la fecha en la lista por importe
    public String getFechaCorta() {
      return gasto.getFecha().format(DIA_CORTO).replace(".", "");
    }

    public String getInicial() {
      String descripcion = gasto.getDescripcion();
      return descripcion == null || descripcion.isBlank()
        ? "?"
        : descripcion.strip().substring(0, 1).toUpperCase(ARGENTINA);
    }

    public String getColor() {
      return ColoresDeCategoria.de(gasto.getCategoria() == null ? null : getCategoria());
    }
  }

  /** Lo gastado en una categoría y el ancho de su barra (relativo a la categoría de más gasto). */
  public static class TotalDeCategoria {

    private final String nombre;
    private final double total;
    private final double maximo;

    TotalDeCategoria(String nombre, double total, double maximo) {
      this.nombre = nombre;
      this.total = total;
      this.maximo = maximo;
    }

    public String getNombre() {
      return nombre;
    }

    public double getTotal() {
      return total;
    }

    public String getColor() {
      return ColoresDeCategoria.de(nombre);
    }

    public long getAncho() {
      return maximo <= 0 ? 0 : Math.round((total * 100) / maximo);
    }
  }
}
