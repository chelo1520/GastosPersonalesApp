package com.tallerwebi.dominio.resumen;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.RepositorioGasto;
import com.tallerwebi.dominio.presupuesto.PresupuestoMensual;
import jakarta.transaction.Transactional;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ServicioResumenImpl
 */
@Service
@Transactional
public class ServicioResumenImpl implements ServicioResumen {

  // Gasto todavía no tiene categoría: todos los gastos van a esta
  static final String SIN_CATEGORIA = "Sin categoría";

  private RepositorioGasto repositorioGasto;

  @Autowired
  public ServicioResumenImpl(RepositorioGasto repositorioGasto) {
    this.repositorioGasto = repositorioGasto;
  }

  @Override
  public ResumenMensual obtenerResumen(Usuario usuario, YearMonth mes) {
    List<Gasto> gastos = repositorioGasto.obtenerGastosDelMes(usuario, mes);

    double gastado = 0.0;
    Map<String, Double> gastadoPorCategoria = new LinkedHashMap<>();
    for (Gasto gasto : gastos) {
      gastado += gasto.getImporte();
      gastadoPorCategoria.merge(SIN_CATEGORIA, gasto.getImporte(), Double::sum);
    }

    double presupuesto = PresupuestoMensual.MONTO;

    // De la que más se gastó a la que menos, como se lee el gráfico de torta.
    // No hay presupuesto por categoría: como hoy todos los gastos van a una sola,
    // esa categoría se compara contra el presupuesto del mes
    List<ResumenCategoria> categorias = new ArrayList<>();
    for (Map.Entry<String, Double> categoria : gastadoPorCategoria.entrySet()) {
      double gastadoCategoria = categoria.getValue();
      categorias.add(
        new ResumenCategoria(
          categoria.getKey(),
          gastadoCategoria,
          porcentaje(gastadoCategoria, gastado),
          presupuesto - gastadoCategoria,
          porcentajeUsado(gastadoCategoria, presupuesto)
        )
      );
    }
    categorias.sort(Comparator.comparing(ResumenCategoria::getGastado).reversed());

    return new ResumenMensual(
      gastado,
      presupuesto,
      presupuesto - gastado,
      porcentajeUsado(gastado, presupuesto),
      categorias
    );
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
