package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorGasto {

  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String REDIRECT_MOSTRAR_GASTOS = "redirect:/mostrar-gastos";
  private GastoServicio gastoServicio;

  @Autowired
  public ControladorGasto(GastoServicio gastoServicio) {
    this.gastoServicio = gastoServicio;
  }

  @RequestMapping(path = "/mostrar-gastos", method = RequestMethod.GET)
  public ModelAndView mostrarGastos(HttpServletRequest request) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    List<Gasto> gastos = gastoServicio
      .obtenerGastos(usuario)
      .stream()
      .sorted(Comparator.comparing(Gasto::getFecha).reversed())
      .toList();
    double total = gastos.stream().mapToDouble(Gasto::getImporte).sum();

    Map<String, Object> model = new ModelMap();
    model.put("gastos", gastos);
    model.put("total", total);

    return new ModelAndView("mostrar-gastos", model);
  }

  @RequestMapping(path = "/registrar-gasto", method = RequestMethod.GET)
  public ModelAndView mostrarFormularioRegistrarGasto(HttpServletRequest request) {
    if (SesionUsuario.obtener(request) == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    Gasto gasto = new Gasto();
    gasto.setFecha(LocalDate.now());

    return vistaRegistrarGasto(gasto, null);
  }

  @RequestMapping(path = "/registrar-gasto", method = RequestMethod.POST)
  public ModelAndView registrarGasto(Gasto gasto, HttpServletRequest request) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    try {
      gastoServicio.registrarGasto(gasto, usuario);
    } catch (GastoInvalidoExeption e) {
      return vistaRegistrarGasto(gasto, e.getMessage());
    }

    return new ModelAndView(REDIRECT_MOSTRAR_GASTOS);
  }

  @RequestMapping(path = "/modificar-gasto/{id}", method = RequestMethod.GET)
  public ModelAndView mostrarFormularioModificarGasto(
    @PathVariable("id") Long id,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    try {
      Gasto gasto = gastoServicio.obtenerGasto(id, usuario);
      return vistaModificarGasto(id, gasto, null);
    } catch (GastoNoEncontrado e) {
      return new ModelAndView(REDIRECT_MOSTRAR_GASTOS);
    }
  }

  @RequestMapping(path = "/modificar-gasto/{id}", method = RequestMethod.POST)
  public ModelAndView modificarGasto(
    @PathVariable("id") Long id,
    Gasto gasto,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    try {
      gastoServicio.modificarGasto(id, gasto, usuario);
    } catch (GastoInvalidoExeption e) {
      return vistaModificarGasto(id, gasto, e.getMessage());
    } catch (GastoNoEncontrado e) {
      return new ModelAndView(REDIRECT_MOSTRAR_GASTOS);
    }

    return new ModelAndView(REDIRECT_MOSTRAR_GASTOS);
  }

  private ModelAndView vistaRegistrarGasto(Gasto gasto, String error) {
    ModelAndView vista = vistaFormularioGasto(gasto, error);
    vista.addObject("titulo", "Registrar gasto");
    vista.addObject("accion", "/registrar-gasto");
    vista.addObject("urlCancelar", "/home");
    vista.addObject("seccion", "registrar");
    return vista;
  }

  private ModelAndView vistaModificarGasto(Long id, Gasto gasto, String error) {
    ModelAndView vista = vistaFormularioGasto(gasto, error);
    vista.addObject("titulo", "Modificar gasto");
    vista.addObject("accion", "/modificar-gasto/" + id);
    vista.addObject("urlCancelar", "/mostrar-gastos");
    vista.addObject("seccion", "mis-gastos");
    return vista;
  }

  // Registrar y modificar comparten el mismo formulario.
  private ModelAndView vistaFormularioGasto(Gasto gasto, String error) {
    Map<String, Object> model = new ModelMap();
    model.put("gasto", gasto);
    model.put("fechaMinima", gastoServicio.obtenerFechaMinimaPermitida());
    model.put("fechaMaxima", gastoServicio.obtenerFechaMaximaPermitida());
    if (error != null) {
      model.put("error", error);
    }
    return new ModelAndView("registrar-gasto", model);
  }

  @RequestMapping(path = "/sumar-gastos", method = RequestMethod.GET)
  public ModelAndView sumarGastos(
    HttpServletRequest request,
    @RequestParam("desde") LocalDate desde,
    @RequestParam("hasta") LocalDate hasta
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    Double total = gastoServicio.sumarGastos(usuario, desde, hasta);

    Map<String, Object> model = new ModelMap();
    model.put("total", total);

    return new ModelAndView("sumar-gastos", model);
  }

  @RequestMapping(path = "/simulacion", method = RequestMethod.GET)
  public ModelAndView mostrarSimulacion(HttpServletRequest request) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
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
    model.put("presupuesto", 0);

    return new ModelAndView("simulacion", model);
  }
}
