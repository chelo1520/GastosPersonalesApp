package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorGasto {

  private static final String REDIRECT_LOGIN = "redirect:/login";
  private GastoServicio gastoServicio;

  @Autowired
  public ControladorGasto(GastoServicio gastoServicio) {
    this.gastoServicio = gastoServicio;
  }

  @RequestMapping(path = "/mostrar-gastos", method = RequestMethod.GET)
  public ModelAndView mostrarGastos(HttpServletRequest request) {
    Usuario usuario = obtenerUsuarioDeSesion(request);
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
    if (obtenerUsuarioDeSesion(request) == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    Gasto gasto = new Gasto();
    gasto.setFecha(LocalDate.now());

    Map<String, Object> model = new ModelMap();
    model.put("gasto", gasto);

    return new ModelAndView("registrar-gasto", model);
  }

  @RequestMapping(path = "/registrar-gasto", method = RequestMethod.POST)
  public ModelAndView registrarGasto(Gasto gasto, HttpServletRequest request) {
    Usuario usuario = obtenerUsuarioDeSesion(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    try {
      gastoServicio.registrarGasto(gasto, usuario);
    } catch (GastoInvalidoExeption e) {
      Map<String, Object> model = new ModelMap();
      model.put("gasto", gasto);
      model.put("error", e.getMessage());
      return new ModelAndView("registrar-gasto", model);
    }

    return new ModelAndView("redirect:/mostrar-gastos");
  }

  @RequestMapping(path = "/sumar-gastos", method = RequestMethod.GET)
  public ModelAndView sumarGastos(
    HttpServletRequest request,
    @RequestParam("desde") LocalDate desde,
    @RequestParam("hasta") LocalDate hasta
  ) {
    Usuario usuario = obtenerUsuarioDeSesion(request);
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
    Usuario usuario = obtenerUsuarioDeSesion(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    YearMonth mesBase = YearMonth.now();
    YearMonth mesSimulado = mesBase.plusMonths(1);

    List<Gasto> gastos = gastoServicio
      .obtenerGastos(usuario)
      .stream()
      .filter(gasto -> gasto.getFecha() != null && YearMonth.from(gasto.getFecha()).equals(mesBase))
      .sorted(Comparator.comparing(Gasto::getFecha).reversed())
      .toList();

    Map<String, Object> model = new ModelMap();
    model.put("gastos", gastos);
    model.put("mesBase", formatearMes(mesBase));
    model.put("mesSimulado", formatearMes(mesSimulado));
    model.put("presupuesto", 0);

    return new ModelAndView("simulacion", model);
  }

  private String formatearMes(YearMonth mes) {
    String nombre = mes.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "AR"));
    return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + mes.getYear();
  }

  private Usuario obtenerUsuarioDeSesion(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }

    Object usuario = session.getAttribute("USUARIO");
    return usuario instanceof Usuario ? (Usuario) usuario : null;
  }
}
