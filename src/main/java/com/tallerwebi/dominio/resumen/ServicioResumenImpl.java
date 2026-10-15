package com.tallerwebi.dominio.resumen;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.alerta.CalculadoraAlertas;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.RepositorioGasto;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ServicioResumenImpl: cruza los gastos del mes con los presupuestos por categoría
 */
@Service
@Transactional
public class ServicioResumenImpl implements ServicioResumen {

  static final String SIN_CATEGORIA = "Sin categoría";

  private RepositorioGasto repositorioGasto;
  private ServicioPresupuesto servicioPresupuesto;
  private Clock reloj;
  private final CalculadoraGastosHormiga calculadoraHormiga = new CalculadoraGastosHormiga();
  private final CalculadoraAlertas calculadoraAlertas = new CalculadoraAlertas();

  @Autowired
  public ServicioResumenImpl(
    RepositorioGasto repositorioGasto,
    ServicioPresupuesto servicioPresupuesto
  ) {
    this(repositorioGasto, servicioPresupuesto, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public ServicioResumenImpl(
    RepositorioGasto repositorioGasto,
    ServicioPresupuesto servicioPresupuesto,
    Clock reloj
  ) {
    this.repositorioGasto = repositorioGasto;
    this.servicioPresupuesto = servicioPresupuesto;
    this.reloj = reloj;
  }

  @Override
  public ResumenMensual obtenerResumen(Usuario usuario, YearMonth mes) {
    List<Gasto> gastos = repositorioGasto.obtenerGastosDelMes(usuario, mes);

    double gastado = 0.0;
    Map<String, Double> gastadoPorCategoria = new LinkedHashMap<>();
    // La entidad de cada fila, para el check "¿Es hormiga?" de la tabla
    Map<String, Categoria> categoriaPorNombre = new LinkedHashMap<>();
    for (Gasto gasto : gastos) {
      gastado += gasto.getImporte();
      String nombreCategoria = nombreDeCategoria(gasto);
      if (gasto.getCategoria() != null) {
        categoriaPorNombre.putIfAbsent(nombreCategoria, gasto.getCategoria());
      }
      gastadoPorCategoria.merge(nombreCategoria, gasto.getImporte(), Double::sum);
    }

    // El presupuesto del mes es la suma de lo definido por categoría
    double presupuesto = 0.0;
    Map<String, Double> presupuestoPorCategoria = new LinkedHashMap<>();
    for (Presupuesto definido : servicioPresupuesto.obtenerDelMes(usuario, mes)) {
      presupuesto += definido.getMonto();
      presupuestoPorCategoria.put(definido.getCategoria().getNombre(), definido.getMonto());
      categoriaPorNombre.putIfAbsent(definido.getCategoria().getNombre(), definido.getCategoria());
    }

    List<ResumenCategoria> categorias = filas(
      gastado,
      gastadoPorCategoria,
      presupuestoPorCategoria,
      categoriaPorNombre
    );

    ResumenHormiga hormiga = calculadoraHormiga.calcular(gastos, presupuesto);

    return new ResumenMensual(
      gastado,
      presupuesto,
      presupuesto - gastado,
      porcentajeUsado(gastado, presupuesto),
      categorias,
      hormiga,
      alertasDelMes(hormiga, mes)
    );
  }

  private List<ResumenCategoria> filas(
    double gastado,
    Map<String, Double> gastadoPorCategoria,
    Map<String, Double> presupuestoPorCategoria,
    Map<String, Categoria> categoriaPorNombre
  ) {
    List<ResumenCategoria> categorias = new ArrayList<>();
    for (Map.Entry<String, Double> categoria : gastadoPorCategoria.entrySet()) {
      double gastadoCategoria = categoria.getValue();
      categorias.add(
        fila(
          categoria.getKey(),
          gastadoCategoria,
          porcentaje(gastadoCategoria, gastado),
          presupuestoPorCategoria.get(categoria.getKey()),
          categoriaPorNombre.get(categoria.getKey())
        )
      );
    }
    // Las categorías con presupuesto pero sin gastos también se muestran (gastado 0)
    for (Map.Entry<String, Double> definido : presupuestoPorCategoria.entrySet()) {
      if (!gastadoPorCategoria.containsKey(definido.getKey())) {
        categorias.add(
          fila(
            definido.getKey(),
            0.0,
            0.0,
            definido.getValue(),
            categoriaPorNombre.get(definido.getKey())
          )
        );
      }
    }
    // De la que más se gastó a la que menos, como se lee el gráfico de torta
    categorias.sort(Comparator.comparing(ResumenCategoria::getGastado).reversed());
    return categorias;
  }

  // Un gasto sin categoría, o con una sin nombre, se agrupa en "Sin categoría"
  private static String nombreDeCategoria(Gasto gasto) {
    String nombre = gasto.getCategoria() == null ? null : gasto.getCategoria().getNombre();
    return nombre == null || nombre.isBlank() ? SIN_CATEGORIA : nombre;
  }

  // Sin categoría, o con una fija (siempre esencial), el check "¿Es hormiga?" no se puede cambiar
  private ResumenCategoria fila(
    String nombre,
    Double gastado,
    Double porcentajeTotal,
    Double presupuesto,
    Categoria categoria
  ) {
    if (categoria == null) {
      return new ResumenCategoria(nombre, gastado, porcentajeTotal, presupuesto);
    }
    Boolean esHormiga = categoria.isFija() ? null : !categoria.isEsencial();
    return new ResumenCategoria(
      nombre,
      gastado,
      porcentajeTotal,
      presupuesto,
      categoria.getId(),
      esHormiga
    );
  }

  // Las alertas hablan de lo que "llevás": solo aplican al mes en curso
  private List<Alerta> alertasDelMes(ResumenHormiga hormiga, YearMonth mes) {
    if (!mes.equals(YearMonth.now(reloj))) {
      return List.of();
    }
    return calculadoraAlertas.alertasDelMes(hormiga);
  }

  @Override
  public List<ResumenDeMes> obtenerHistorial(Usuario usuario, YearMonth hasta, int cantidadMeses) {
    YearMonth desde = hasta.minusMonths(cantidadMeses - 1L);
    // Una sola consulta para todos los meses; después se reparte por mes
    Map<YearMonth, List<Gasto>> gastosPorMes = new LinkedHashMap<>();
    for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
      gastosPorMes.put(mes, new ArrayList<>());
    }
    for (Gasto gasto : repositorioGasto.obtenerGastosEntreFechas(
      usuario,
      desde.atDay(1),
      hasta.atEndOfMonth()
    )) {
      List<Gasto> delMes = gastosPorMes.get(YearMonth.from(gasto.getFecha()));
      if (delMes != null) {
        delMes.add(gasto);
      }
    }

    YearMonth mesActual = YearMonth.now(reloj);
    List<ResumenDeMes> historial = new ArrayList<>();
    for (Map.Entry<YearMonth, List<Gasto>> mes : gastosPorMes.entrySet()) {
      double presupuesto = presupuestoDe(usuario, mes.getKey());
      double gastado = mes.getValue().stream().mapToDouble(Gasto::getImporte).sum();
      // "Chico" es el 2 % del presupuesto de ese mes, no del actual
      double hormiga = calculadoraHormiga.calcular(mes.getValue(), presupuesto).getTotal();
      historial.add(
        new ResumenDeMes(
          mes.getKey(),
          gastado,
          hormiga,
          presupuesto,
          mes.getKey().equals(mesActual)
        )
      );
    }
    return historial;
  }

  // El presupuesto de cada mes del historial: la suma de lo definido por categoría ese mes
  private double presupuestoDe(Usuario usuario, YearMonth mes) {
    return servicioPresupuesto.totalDelMes(usuario, mes);
  }

  private double porcentaje(double parte, double total) {
    return total > 0 ? (parte / total) * 100 : 0.0;
  }

  // Sin presupuesto, cualquier gasto ya lo excede: se muestra el 100 % usado
  private double porcentajeUsado(double gastado, double presupuesto) {
    if (presupuesto > 0) {
      return (gastado / presupuesto) * 100;
    }
    return gastado > 0 ? 100.0 : 0.0;
  }
}
