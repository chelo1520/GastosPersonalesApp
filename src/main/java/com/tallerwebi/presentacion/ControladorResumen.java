package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.NombreDeMes;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.resumen.ResumenDeMes;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * ControladorResumen
 */
@Controller
public class ControladorResumen {

  // 12 barras en el celular quedan muy finas
  static final int MESES_HISTORIAL = 6;
  // Últimos movimientos que se muestran en el resumen
  static final int MOVIMIENTOS_RECIENTES = 5;
  private ServicioResumen servicioResumen;
  private ServicioPresupuesto servicioPresupuesto;
  private GastoServicio gastoServicio;
  private Clock reloj;

  @Autowired
  public ControladorResumen(
    ServicioResumen servicioResumen,
    ServicioPresupuesto servicioPresupuesto,
    GastoServicio gastoServicio
  ) {
    this(servicioResumen, servicioPresupuesto, gastoServicio, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public ControladorResumen(
    ServicioResumen servicioResumen,
    ServicioPresupuesto servicioPresupuesto,
    GastoServicio gastoServicio,
    Clock reloj
  ) {
    this.servicioResumen = servicioResumen;
    this.servicioPresupuesto = servicioPresupuesto;
    this.gastoServicio = gastoServicio;
    this.reloj = reloj;
  }

  private List<MovimientoReciente> movimientosRecientes(Usuario usuario) {
    LocalDate hoy = LocalDate.now(reloj);
    List<MovimientoReciente> movimientos = new ArrayList<>();
    for (Gasto gasto : gastoServicio.obtenerUltimos(usuario, MOVIMIENTOS_RECIENTES)) {
      movimientos.add(new MovimientoReciente(gasto, hoy));
    }
    return movimientos;
  }

  // mes: opcional, con formato yyyy-MM (ej. 2026-09). Sin mes, o con uno inválido, muestra el actual
  @RequestMapping(path = "/home", method = RequestMethod.GET)
  public ModelAndView mostrarResumen(
    @RequestParam(value = "mes", required = false) String mes,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView("redirect:/login");
    }
    YearMonth mesMostrado = elegirMes(mes);
    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, mesMostrado);
    Map<String, Object> model = new ModelMap();
    // Saludo por el nombre si el usuario lo cargó en su perfil
    model.put("nombre", usuario.getNombre());
    model.put("mesActual", NombreDeMes.de(mesMostrado));
    model.put("mesAnterior", mesMostrado.minusMonths(1).toString());
    model.put("mesSiguiente", mesMostrado.plusMonths(1).toString());
    model.put("mesMostrado", mesMostrado.toString());
    model.put("gastado", resumen.getGastado());
    model.put("presupuesto", resumen.getPresupuesto());
    model.put("disponible", resumen.getDisponible());
    model.put("porcentajeUsado", resumen.getPorcentajeUsado());
    model.put("categorias", resumen.getCategorias());
    model.put("hormiga", resumen.getHormiga());
    // Qué parte de lo gastado se fue en hormiga (la tarjeta lo dice así, no contra el presupuesto)
    model.put(
      "hormigaPorcentajeGastado",
      resumen.getGastado() > 0
        ? (resumen.getHormiga().getTotal() / resumen.getGastado()) * 100
        : 0.0
    );
    model.put("movimientos", movimientosRecientes(usuario));
    // Para el modal de análisis: "Con tus datos al dd/MM/yyyy · N movimientos"
    model.put(
      "cantidadMovimientos",
      gastoServicio.obtenerGastosDelMes(usuario, mesMostrado).size()
    );
    model.put("hoyTexto", LocalDate.now(reloj).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    model.put("alertas", resumen.getAlertas());
    model.put("historial", historial(usuario, mesMostrado));
    // Total del mes (el definido para ese mes o el del perfil), contra lo gastado
    double presupuestoMensual = servicioPresupuesto.presupuestoDelMes(usuario, mesMostrado);
    model.put("presupuestoMensual", presupuestoMensual);
    model.put("disponibleMensual", presupuestoMensual - resumen.getGastado());
    model.put("excedido", resumen.getGastado() > presupuestoMensual && presupuestoMensual > 0);
    model.put("asignado", resumen.getPresupuesto());
    model.put("sinAsignar", presupuestoMensual - resumen.getPresupuesto());
    model.put(
      "porcentajeMensualUsado",
      presupuestoMensual > 0 ? (resumen.getGastado() * 100) / presupuestoMensual : 0.0
    );
    return new ModelAndView("resumen", model);
  }

  // Los 6 meses que terminan en el mes que se está mirando
  private List<MesDelHistorial> historial(Usuario usuario, YearMonth mesMostrado) {
    List<ResumenDeMes> meses = servicioResumen.obtenerHistorial(
      usuario,
      mesMostrado,
      MESES_HISTORIAL
    );
    return meses == null ? List.of() : meses.stream().map(MesDelHistorial::new).toList();
  }

  private YearMonth elegirMes(String mes) {
    if (mes == null || mes.isBlank()) {
      return YearMonth.now(reloj);
    }
    try {
      return YearMonth.parse(mes);
    } catch (DateTimeParseException e) {
      return YearMonth.now(reloj);
    }
  }
}
