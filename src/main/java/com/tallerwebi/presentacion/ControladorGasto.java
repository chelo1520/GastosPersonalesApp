package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorGasto {

  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String REDIRECT_MOSTRAR_GASTOS = "redirect:/mostrar-gastos";
  private GastoServicio gastoServicio;
  private CategoriaRepositorio categoriaRepositorio;
  private ServicioPresupuesto servicioPresupuesto;

  @Autowired
  public ControladorGasto(
    GastoServicio gastoServicio,
    CategoriaRepositorio categoriaRepositorio,
    ServicioPresupuesto servicioPresupuesto
  ) {
    this.gastoServicio = gastoServicio;
    this.categoriaRepositorio = categoriaRepositorio;
    this.servicioPresupuesto = servicioPresupuesto;
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
  public ModelAndView registrarGasto(
    Gasto gasto,
    HttpServletRequest request,
    RedirectAttributes redirectAttributes
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    try {
      String errorCategoria = asignarCategoriaExistente(gasto);
      if (errorCategoria != null) {
        return vistaRegistrarGasto(gasto, errorCategoria);
      }
      gastoServicio.registrarGasto(gasto, usuario);
    } catch (GastoInvalidoExeption e) {
      return vistaRegistrarGasto(gasto, e.getMessage());
    }

    // Se muestra una sola vez, en la lista, después de guardar
    Alerta alerta = gastoServicio.alertaDeRepeticion(gasto, usuario);
    if (alerta != null) {
      redirectAttributes.addFlashAttribute("alertaRepeticion", alerta);
    }
    // Pop-up, también una sola vez: solo con el gasto que cruza el 80 % o el 100 %
    Alerta alertaPresupuesto = gastoServicio.alertaDePresupuesto(gasto, usuario);
    if (alertaPresupuesto != null) {
      redirectAttributes.addFlashAttribute("alertaPresupuesto", alertaPresupuesto);
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
      String errorCategoria = asignarCategoriaExistente(gasto);
      if (errorCategoria != null) {
        return vistaModificarGasto(id, gasto, errorCategoria);
      }
      gastoServicio.modificarGasto(id, gasto, usuario);
    } catch (GastoInvalidoExeption e) {
      return vistaModificarGasto(id, gasto, e.getMessage());
    } catch (GastoNoEncontrado e) {
      return new ModelAndView(REDIRECT_MOSTRAR_GASTOS);
    }

    return new ModelAndView(REDIRECT_MOSTRAR_GASTOS);
  }

  @RequestMapping(path = "/eliminar-gasto/{id}", method = RequestMethod.POST)
  public ModelAndView eliminarGasto(@PathVariable("id") Long id, HttpServletRequest request) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    try {
      gastoServicio.eliminarGasto(id, usuario);
    } catch (GastoNoEncontrado e) {
      // Un gasto inexistente o de otro usuario no se elimina: se vuelve a la lista igual
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
    model.put("categorias", categoriaRepositorio.obtenerTodas());
    model.put("fechaMinima", gastoServicio.obtenerFechaMinimaPermitida());
    model.put("fechaMaxima", gastoServicio.obtenerFechaMaximaPermitida());
    if (error != null) {
      model.put("error", error);
    }
    return new ModelAndView("registrar-gasto", model);
  }

  private String asignarCategoriaExistente(Gasto gasto) {
    Categoria seleccionada = gasto.getCategoria();
    if (seleccionada == null || seleccionada.getId() == null) {
      return "Seleccioná una categoría";
    }

    Categoria categoriaExistente = categoriaRepositorio.buscarPorId(seleccionada.getId());
    if (categoriaExistente == null) {
      return "La categoría seleccionada no existe";
    }

    gasto.setCategoria(categoriaExistente);
    return null;
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
    // Presupuesto del mes simulado; si todavía no se definió, se toma el del mes actual
    Double presupuesto = servicioPresupuesto.totalDelMes(usuario, mesSimulado);
    if (presupuesto <= 0) {
      presupuesto = servicioPresupuesto.totalDelMes(usuario, mesActual);
    }
    model.put("presupuesto", presupuesto);

    return new ModelAndView("simulacion", model);
  }
}
