package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.NombreDeMes;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Los gastos del usuario: listarlos, registrarlos a mano, modificarlos y eliminarlos. */
@Controller
public class ControladorGasto {

  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String REDIRECT_MOSTRAR_GASTOS = "redirect:/mostrar-gastos";
  private static final String RUTA_REGISTRAR_GASTO = "/registrar-gasto";
  private static final String CLAVE_TITULO = "titulo";
  private static final String CLAVE_URL_CANCELAR = "urlCancelar";
  private static final String CLAVE_SECCION = "seccion";
  private static final String PARAMETRO_MES = "mes";
  private final GastoServicio gastoServicio;
  private final CategoriaRepositorio categoriaRepositorio;
  private final Clock reloj;

  @Autowired
  public ControladorGasto(GastoServicio gastoServicio, CategoriaRepositorio categoriaRepositorio) {
    this(gastoServicio, categoriaRepositorio, Clock.systemDefaultZone());
  }

  // Con reloj fijo para los tests: "hoy" decide el mes por defecto, los días futuros y "Hoy"/"Ayer"
  public ControladorGasto(
    GastoServicio gastoServicio,
    CategoriaRepositorio categoriaRepositorio,
    Clock reloj
  ) {
    this.gastoServicio = gastoServicio;
    this.categoriaRepositorio = categoriaRepositorio;
    this.reloj = reloj;
  }

  // Mis gastos de un mes (rediseño v2): mapa de calor, lista por día y totales por categoría.
  // mes: opcional, yyyy-MM (ej. 2026-09). Sin mes, o con uno inválido, muestra el actual
  @RequestMapping(path = "/mostrar-gastos", method = RequestMethod.GET)
  public ModelAndView mostrarGastos(HttpServletRequest request) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    YearMonth mes = elegirMes(request.getParameter(PARAMETRO_MES));
    MesDeGastos mesDeGastos = new MesDeGastos(
      gastoServicio.obtenerGastosDelMes(usuario, mes),
      mes,
      LocalDate.now(reloj)
    );

    Map<String, Object> model = new ModelMap();
    model.put("mesDeGastos", mesDeGastos);
    model.put("total", mesDeGastos.getTotal());
    model.put("mesActual", NombreDeMes.de(mes));
    model.put("mesMostrado", mes.toString());
    model.put("mesAnterior", mes.minusMonths(1).toString());
    model.put("mesSiguiente", mes.plusMonths(1).toString());

    return new ModelAndView("mostrar-gastos", model);
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

  // Después de guardar se vuelve al mes del gasto, así se ve dónde quedó
  private ModelAndView volverAlMes(YearMonth mes) {
    if (mes.equals(YearMonth.now(reloj))) {
      return new ModelAndView(REDIRECT_MOSTRAR_GASTOS);
    }
    return new ModelAndView(REDIRECT_MOSTRAR_GASTOS + "?" + PARAMETRO_MES + "=" + mes);
  }

  @RequestMapping(path = RUTA_REGISTRAR_GASTO, method = RequestMethod.GET)
  public ModelAndView mostrarFormularioRegistrarGasto(HttpServletRequest request) {
    if (SesionUsuario.obtener(request) == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    Gasto gasto = new Gasto();
    gasto.setFecha(LocalDate.now());

    return vistaRegistrarGasto(gasto, null);
  }

  @RequestMapping(path = RUTA_REGISTRAR_GASTO, method = RequestMethod.POST)
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

    return volverAlMes(YearMonth.from(gasto.getFecha()));
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

    return volverAlMes(YearMonth.from(gasto.getFecha()));
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

    // El formulario manda el mes que se estaba mirando, para volver a él
    return volverAlMes(elegirMes(request.getParameter(PARAMETRO_MES)));
  }

  private ModelAndView vistaRegistrarGasto(Gasto gasto, String error) {
    ModelAndView vista = vistaFormularioGasto(gasto, error);
    vista.addObject(CLAVE_TITULO, "Registrar gasto");
    vista.addObject("accion", RUTA_REGISTRAR_GASTO);
    vista.addObject(CLAVE_URL_CANCELAR, "/home");
    vista.addObject(CLAVE_SECCION, "registrar");
    vista.addObject("mostrarComprobante", true);
    return vista;
  }

  private ModelAndView vistaModificarGasto(Long id, Gasto gasto, String error) {
    ModelAndView vista = vistaFormularioGasto(gasto, error);
    vista.addObject(CLAVE_TITULO, "Modificar gasto");
    vista.addObject("accion", "/modificar-gasto/" + id);
    vista.addObject(CLAVE_URL_CANCELAR, "/mostrar-gastos");
    vista.addObject(CLAVE_SECCION, "mis-gastos");
    vista.addObject("mostrarComprobante", false);
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
}
