package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.NombreDeMes;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/** Simulación del mes próximo con los gastos del último mes cerrado. */
@Controller
public class ControladorSimulacion {

  private final GastoServicio gastoServicio;
  private final ServicioPresupuesto servicioPresupuesto;

  @Autowired
  public ControladorSimulacion(
    GastoServicio gastoServicio,
    ServicioPresupuesto servicioPresupuesto
  ) {
    this.gastoServicio = gastoServicio;
    this.servicioPresupuesto = servicioPresupuesto;
  }

  @RequestMapping(path = "/simulacion", method = RequestMethod.GET)
  public ModelAndView mostrarSimulacion(HttpServletRequest request) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView("redirect:/login");
    }

    // Se simula el mes próximo tomando como base los gastos del mes anterior (último mes cerrado)
    YearMonth mesActual = YearMonth.now();
    YearMonth mesBase = mesActual.minusMonths(1);
    YearMonth mesSimulado = mesActual.plusMonths(1);

    List<Gasto> gastos = gastoServicio.obtenerGastosDelMesAnterior(usuario, mesActual);

    Map<String, Object> model = new ModelMap();
    model.put("gastos", gastos);
    model.put("mesBase", NombreDeMes.de(mesBase));
    model.put("mesSimulado", NombreDeMes.de(mesSimulado));
    // Presupuesto del mes simulado; si todavía no se definió, se toma el del mes actual
    Double presupuesto = servicioPresupuesto.presupuestoDelMes(usuario, mesSimulado);
    if (presupuesto <= 0) {
      presupuesto = servicioPresupuesto.presupuestoDelMes(usuario, mesActual);
    }
    model.put("presupuesto", presupuesto);

    return new ModelAndView("simulacion", model);
  }
}
