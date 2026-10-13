package com.tallerwebi.dominio.resumen;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.RepositorioGasto;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
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

  @Autowired
  public ServicioResumenImpl(
    RepositorioGasto repositorioGasto,
    ServicioPresupuesto servicioPresupuesto
  ) {
    this.repositorioGasto = repositorioGasto;
    this.servicioPresupuesto = servicioPresupuesto;
  }

  @Override
  public ResumenMensual obtenerResumen(Usuario usuario, YearMonth mes) {
    List<Gasto> gastos = repositorioGasto.obtenerGastosDelMes(usuario, mes);

    double gastado = 0.0;
    Map<String, Double> gastadoPorCategoria = new LinkedHashMap<>();
    for (Gasto gasto : gastos) {
      gastado += gasto.getImporte();
      String nombreCategoria = gasto.getCategoria() == null
        ? null
        : gasto.getCategoria().getNombre();
      if (nombreCategoria == null || nombreCategoria.isBlank()) {
        nombreCategoria = SIN_CATEGORIA;
      }
      gastadoPorCategoria.merge(nombreCategoria, gasto.getImporte(), Double::sum);
    }

    // El presupuesto del mes es la suma de lo definido por categoría
    double presupuesto = 0.0;
    Map<String, Double> presupuestoPorCategoria = new LinkedHashMap<>();
    for (Presupuesto definido : servicioPresupuesto.obtenerDelMes(usuario, mes)) {
      presupuesto += definido.getMonto();
      presupuestoPorCategoria.put(definido.getCategoria().getNombre(), definido.getMonto());
    }

    List<ResumenCategoria> categorias = new ArrayList<>();
    for (Map.Entry<String, Double> categoria : gastadoPorCategoria.entrySet()) {
      double gastadoCategoria = categoria.getValue();
      categorias.add(
        new ResumenCategoria(
          categoria.getKey(),
          gastadoCategoria,
          porcentaje(gastadoCategoria, gastado),
          presupuestoPorCategoria.get(categoria.getKey())
        )
      );
    }
    // Las categorías con presupuesto pero sin gastos también se muestran (gastado 0)
    for (Map.Entry<String, Double> definido : presupuestoPorCategoria.entrySet()) {
      if (!gastadoPorCategoria.containsKey(definido.getKey())) {
        categorias.add(new ResumenCategoria(definido.getKey(), 0.0, 0.0, definido.getValue()));
      }
    }
    // De la que más se gastó a la que menos, como se lee el gráfico de torta
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
