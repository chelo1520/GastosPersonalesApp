package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.NombreDeMes;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.simulacion.EstadoSimulacion;
import com.tallerwebi.dominio.simulacion.ServicioSimulacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

/** Compara los gastos de un mes base con el presupuesto del mes que se quiere simular. */
@Controller
public class ControladorSimulacion {

  private static final int MESES_DE_RANGO = 24;
  private static final int CANTIDAD_MESES = 2;
  private final GastoServicio gastoServicio;
  private final ServicioPresupuesto servicioPresupuesto;
  private final ServicioSimulacion servicioSimulacion;

  @Autowired
  public ControladorSimulacion(
    GastoServicio gastoServicio,
    ServicioPresupuesto servicioPresupuesto,
    ServicioSimulacion servicioSimulacion
  ) {
    this.gastoServicio = gastoServicio;
    this.servicioPresupuesto = servicioPresupuesto;
    this.servicioSimulacion = servicioSimulacion;
  }

  @RequestMapping(path = "/simulacion", method = RequestMethod.GET)
  public ModelAndView mostrarSimulacion(
    @RequestParam(value = "mesBase", required = false) String mesBaseSolicitado,
    @RequestParam(value = "mesSimulado", required = false) String mesSimuladoSolicitado,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView("redirect:/login");
    }

    YearMonth mesActual = YearMonth.now();
    HttpSession sesion = request.getSession(false);
    String baseSolicitada = mesBaseSolicitado;
    String simulacionSolicitada = mesSimuladoSolicitado;
    String seleccionAnterior = (String) sesion.getAttribute("simulacionMeses:" + usuario.getId());
    if (mesBaseSolicitado == null && mesSimuladoSolicitado == null && seleccionAnterior != null) {
      String[] meses = seleccionAnterior.split(",");
      if (meses.length == CANTIDAD_MESES) {
        baseSolicitada = meses[0];
        simulacionSolicitada = meses[1];
      }
    }
    YearMonth mesBase = elegirMes(baseSolicitada, mesActual.minusMonths(1));
    YearMonth mesSimulado = elegirMes(simulacionSolicitada, mesActual.plusMonths(1));
    validarMeses(usuario, mesBase, mesSimulado, mesActual);
    sesion.setAttribute("simulacionMeses:" + usuario.getId(), mesBase + "," + mesSimulado);

    List<Gasto> gastos = gastoServicio.obtenerGastosDelMes(usuario, mesBase);
    EstadoSimulacion estado = servicioSimulacion.obtener(usuario, mesBase, mesSimulado);

    Map<String, Object> model = new ModelMap();
    model.put("estadoSimulacion", estado);
    model.put("gastos", gastos);
    model.put("mesBase", mesBase.toString());
    model.put("mesBaseElegido", mesBase);
    model.put("mesBaseNombre", NombreDeMes.de(mesBase));
    model.put("mesSimulado", mesSimulado.toString());
    model.put("mesSimuladoElegido", mesSimulado);
    model.put("mesSimuladoNombre", NombreDeMes.de(mesSimulado));
    Set<YearMonth> mesesBase = new TreeSet<>(
      mesesDisponibles(mesActual.minusMonths(MESES_DE_RANGO), mesActual, mesBase)
    );
    mesesBase.addAll(servicioSimulacion.mesesBaseGuardados(usuario));
    Set<YearMonth> mesesSimulados = new TreeSet<>(
      mesesDisponibles(mesActual, mesActual.plusMonths(MESES_DE_RANGO), mesSimulado)
    );
    mesesSimulados.addAll(servicioSimulacion.mesesSimuladosGuardados(usuario, mesBase));
    model.put("mesesBase", mesesBase);
    model.put("mesesSimulados", mesesSimulados);
    model.put("mesActual", mesActual.toString());
    // Presupuesto del mes simulado; si todavía no se definió, se toma el del mes actual
    Double presupuesto = servicioPresupuesto.presupuestoDelMes(usuario, mesSimulado);
    if (presupuesto <= 0) {
      presupuesto = servicioPresupuesto.presupuestoDelMes(usuario, mesActual);
    }
    model.put("presupuesto", presupuesto);

    return new ModelAndView("simulacion", model);
  }

  @RequestMapping(path = "/simulacion/guardar", method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity<Void> guardar(
    @RequestParam("mesBase") String base,
    @RequestParam("mesSimulado") String simulado,
    @RequestBody EstadoSimulacion estado,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    YearMonth mesBase = elegirMes(base, null);
    YearMonth mesSimulado = elegirMes(simulado, null);
    if (mesBase == null || mesSimulado == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los meses son obligatorios");
    }
    validarMeses(usuario, mesBase, mesSimulado, YearMonth.now());
    servicioSimulacion.guardar(usuario, mesBase, mesSimulado, estado);
    return ResponseEntity.noContent().build();
  }

  private void validarMeses(
    Usuario usuario,
    YearMonth mesBase,
    YearMonth mesSimulado,
    YearMonth mesActual
  ) {
    if (mesBase.isAfter(mesActual)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "El mes base no puede ser posterior al mes actual"
      );
    }
    if (
      mesSimulado.isBefore(mesActual) && !servicioSimulacion.existe(usuario, mesBase, mesSimulado)
    ) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "No se puede crear una simulación para un mes pasado"
      );
    }
  }

  private YearMonth elegirMes(String valor, YearMonth predeterminado) {
    if (valor == null || valor.isBlank()) {
      return predeterminado;
    }
    try {
      return YearMonth.parse(valor);
    } catch (DateTimeParseException e) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "El mes debe tener formato AAAA-MM",
        e
      );
    }
  }

  private List<YearMonth> mesesDisponibles(YearMonth desde, YearMonth hasta, YearMonth elegido) {
    List<YearMonth> meses = new ArrayList<>();
    if (elegido.isBefore(desde)) {
      meses.add(elegido);
    }
    for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
      meses.add(mes);
    }
    if (elegido.isAfter(hasta)) {
      meses.add(elegido);
    }
    return meses;
  }
}
