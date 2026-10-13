package com.tallerwebi.dominio.asistente;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import com.tallerwebi.dominio.indicadores.ServicioIndicadores;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenDeMes;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ServicioGastoRealImpl: orquesta el resumen (gastos) y los indicadores (IPC, dólar).
 * Todo el cálculo es puro Java sobre esos datos.
 */
@Service
@Transactional
public class ServicioGastoRealImpl implements ServicioGastoReal {

  // Hacen falta dos meses cerrados para comparar
  private static final int MINIMO_MESES_CERRADOS = 2;

  private final ServicioResumen servicioResumen;
  private final ServicioIndicadores servicioIndicadores;
  private final Clock reloj;

  @Autowired
  public ServicioGastoRealImpl(
    ServicioResumen servicioResumen,
    ServicioIndicadores servicioIndicadores
  ) {
    this(servicioResumen, servicioIndicadores, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public ServicioGastoRealImpl(
    ServicioResumen servicioResumen,
    ServicioIndicadores servicioIndicadores,
    Clock reloj
  ) {
    this.servicioResumen = servicioResumen;
    this.servicioIndicadores = servicioIndicadores;
    this.reloj = reloj;
  }

  @Override
  public GastoReal calcular(Usuario usuario, int cantidadMeses) {
    YearMonth mesActual = YearMonth.now(reloj);
    List<ResumenDeMes> historial = servicioResumen.obtenerHistorial(
      usuario,
      mesActual,
      cantidadMeses
    );

    Map<YearMonth, InflacionMensual> ipcPorMes = new LinkedHashMap<>();
    for (ResumenDeMes mes : historial) {
      ipcPorMes.put(mes.getMes(), servicioIndicadores.inflacionMensual(mes.getMes()));
    }

    List<MesGastoReal> meses = mesesEnPesosDeHoy(historial, ipcPorMes, mesActual);
    List<MesGastoReal> cerrados = meses.stream().filter(mes -> !mes.isEnCurso()).toList();
    YearMonth ultimoCerrado = cerrados.isEmpty()
      ? null
      : cerrados.get(cerrados.size() - 1).getMes();

    return new GastoReal(
      meses,
      veredicto(cerrados, ipcPorMes),
      ultimoCerrado,
      ultimoCerrado == null
        ? List.of()
        : categorias(usuario, ultimoCerrado, ipcPorMes.get(ultimoCerrado)),
      servicioIndicadores.ultimaActualizacion(),
      servicioIndicadores.datosActualizados()
    );
  }

  // Pesos de hoy: cada mes se multiplica por la inflación acumulada desde el mes siguiente hasta hoy
  private List<MesGastoReal> mesesEnPesosDeHoy(
    List<ResumenDeMes> historial,
    Map<YearMonth, InflacionMensual> ipcPorMes,
    YearMonth mesActual
  ) {
    List<MesGastoReal> meses = new ArrayList<>();
    double factor = 1.0;
    for (int i = historial.size() - 1; i >= 0; i--) {
      ResumenDeMes mes = historial.get(i);
      InflacionMensual ipc = ipcPorMes.get(mes.getMes());
      boolean enCurso = mes.getMes().equals(mesActual);
      double dolar = enCurso
        ? servicioIndicadores.dolarDeHoy().getVenta()
        : servicioIndicadores.cotizacionDolar(mes.getMes().atEndOfMonth()).getVenta();
      meses.add(
        0,
        new MesGastoReal(
          mes.getMes(),
          mes.getGastado(),
          mes.getGastado() * factor,
          ipc.getPorcentaje(),
          ipc.isEstimada(),
          dolar,
          enCurso,
          mes.getHormiga(),
          mes.getPresupuesto()
        )
      );
      // El IPC de este mes es lo que separa sus pesos de los del mes anterior
      factor *= 1 + ipc.getPorcentaje() / 100;
    }
    return meses;
  }

  // Compara el primer y el último mes cerrado del período; el mes en curso no cuenta
  private Veredicto veredicto(
    List<MesGastoReal> cerrados,
    Map<YearMonth, InflacionMensual> ipcPorMes
  ) {
    if (cerrados.size() < MINIMO_MESES_CERRADOS) {
      return null;
    }
    MesGastoReal primero = cerrados.get(0);
    MesGastoReal ultimo = cerrados.get(cerrados.size() - 1);
    if (primero.getGastado() <= 0) {
      return null;
    }
    double variacionNominal = (ultimo.getGastado() / primero.getGastado() - 1) * 100;
    double acumulada = 1.0;
    for (
      YearMonth mes = primero.getMes().plusMonths(1);
      !mes.isAfter(ultimo.getMes());
      mes = mes.plusMonths(1)
    ) {
      acumulada *= 1 + ipcPorMes.get(mes).getPorcentaje() / 100;
    }
    return new Veredicto(
      primero.getMes(),
      ultimo.getMes(),
      variacionNominal,
      (acumulada - 1) * 100
    );
  }

  // Último mes cerrado contra el anterior, categoría por categoría, descontando el IPC del mes
  private List<CategoriaGastoReal> categorias(
    Usuario usuario,
    YearMonth ultimoCerrado,
    InflacionMensual ipcDelMes
  ) {
    Map<String, Double> actual = gastadoPorCategoria(usuario, ultimoCerrado);
    Map<String, Double> anterior = gastadoPorCategoria(usuario, ultimoCerrado.minusMonths(1));

    Set<String> nombres = new LinkedHashSet<>(actual.keySet());
    nombres.addAll(anterior.keySet());

    List<CategoriaGastoReal> categorias = new ArrayList<>();
    for (String nombre : nombres) {
      categorias.add(
        new CategoriaGastoReal(
          nombre,
          actual.getOrDefault(nombre, 0.0),
          anterior.getOrDefault(nombre, 0.0),
          ipcDelMes.getPorcentaje()
        )
      );
    }
    // Las que más subieron en serio, primero; las que no se pueden comparar, al final
    categorias.sort(
      Comparator.comparing(
        CategoriaGastoReal::getVariacionReal,
        Comparator.nullsLast(Comparator.reverseOrder())
      )
    );
    return categorias;
  }

  private Map<String, Double> gastadoPorCategoria(Usuario usuario, YearMonth mes) {
    Map<String, Double> porCategoria = new LinkedHashMap<>();
    for (ResumenCategoria categoria : servicioResumen
      .obtenerResumen(usuario, mes)
      .getCategorias()) {
      if (categoria.getGastado() > 0) {
        porCategoria.put(categoria.getNombre(), categoria.getGastado());
      }
    }
    return porCategoria;
  }
}
