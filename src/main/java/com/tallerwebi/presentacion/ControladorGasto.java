package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobanteNoDisponible;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorGasto {

  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String REDIRECT_MOSTRAR_GASTOS = "redirect:/mostrar-gastos";
  private static final String RUTA_REGISTRAR_GASTO = "/registrar-gasto";
  private static final String RUTA_COMPROBANTE = RUTA_REGISTRAR_GASTO + "/comprobante";
  private static final String CLAVE_TITULO = "titulo";
  private static final String CLAVE_URL_CANCELAR = "urlCancelar";
  private static final String CLAVE_SECCION = "seccion";
  private static final byte[] FIRMA_PNG = { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
  private static final byte[] FIRMA_JPEG = { (byte) 0xff, (byte) 0xd8, (byte) 0xff };
  private static final byte[] FIRMA_PDF = { 0x25, 0x50, 0x44, 0x46 };
  private static final String FORMATOS_ACEPTADOS = "JPG, PNG o PDF";
  private static final long MAX_IMAGEN_BYTES = 5L * 1024 * 1024;
  private static final String VISTA_COMPROBANTE = "cargar-comprobante";
  private final GastoServicio gastoServicio;
  private final CategoriaRepositorio categoriaRepositorio;
  private final ServicioPresupuesto servicioPresupuesto;
  private final LectorComprobante lectorComprobante;

  @Autowired
  public ControladorGasto(
    GastoServicio gastoServicio,
    CategoriaRepositorio categoriaRepositorio,
    ServicioPresupuesto servicioPresupuesto,
    LectorComprobante lectorComprobante
  ) {
    this.gastoServicio = gastoServicio;
    this.categoriaRepositorio = categoriaRepositorio;
    this.servicioPresupuesto = servicioPresupuesto;
    this.lectorComprobante = lectorComprobante;
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

  @RequestMapping(path = RUTA_REGISTRAR_GASTO, method = RequestMethod.GET)
  public ModelAndView mostrarFormularioRegistrarGasto(HttpServletRequest request) {
    if (SesionUsuario.obtener(request) == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    Gasto gasto = new Gasto();
    gasto.setFecha(LocalDate.now());

    return vistaRegistrarGasto(gasto, null);
  }

  @RequestMapping(path = RUTA_COMPROBANTE, method = RequestMethod.GET)
  public ModelAndView mostrarCargaComprobante(HttpServletRequest request) {
    if (SesionUsuario.obtener(request) == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    return vistaCargaComprobante(null);
  }

  @RequestMapping(path = RUTA_COMPROBANTE, method = RequestMethod.POST)
  public ModelAndView procesarComprobante(
    @RequestParam(value = "imagen", required = false) MultipartFile imagen,
    HttpServletRequest request
  ) {
    if (SesionUsuario.obtener(request) == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    if (imagen == null || imagen.isEmpty()) {
      return vistaCargaComprobante("Elegí un archivo " + FORMATOS_ACEPTADOS + " para continuar.");
    }
    if (imagen.getSize() > MAX_IMAGEN_BYTES) {
      return vistaCargaComprobante("El archivo supera los 5 MB. Elegí otro.");
    }

    byte[] contenido;
    try {
      contenido = imagen.getBytes();
    } catch (IOException e) {
      return vistaCargaComprobante("No se pudo leer el archivo. Elegí otra imagen.");
    }

    String tipoArchivo = detectarTipoArchivo(contenido);
    if (tipoArchivo == null) {
      return vistaCargaComprobante("El archivo debe ser " + FORMATOS_ACEPTADOS + " de hasta 5 MB.");
    }

    try {
      List<Categoria> categorias = categoriaRepositorio.obtenerTodas();
      List<String> nombresCategorias = categorias
        .stream()
        .map(categoria -> categoria.getNombre())
        .toList();
      DatosComprobante datos = lectorComprobante.leer(contenido, tipoArchivo, nombresCategorias);
      return vistaRevisionComprobante(datos, categorias);
    } catch (ComprobanteNoLegible | LectorComprobanteNoDisponible e) {
      return vistaCargaComprobante(e.getMessage());
    }
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

  private ModelAndView vistaCargaComprobante(String error) {
    Map<String, Object> model = new ModelMap();
    model.put(CLAVE_TITULO, "Desde comprobante");
    model.put(CLAVE_SECCION, "registrar");
    model.put(CLAVE_URL_CANCELAR, RUTA_REGISTRAR_GASTO);
    if (error != null) {
      model.put("error", error);
    }
    return new ModelAndView(VISTA_COMPROBANTE, model);
  }

  private ModelAndView vistaRevisionComprobante(
    DatosComprobante datos,
    List<Categoria> categorias
  ) {
    Gasto gasto = new Gasto(datos.importe(), datos.fecha(), datos.descripcion());
    Categoria categoriaSugerida = categorias
      .stream()
      .filter(categoria -> datos.categoria() != null)
      .filter(categoria -> categoria.getNombre().equalsIgnoreCase(datos.categoria().trim()))
      .findFirst()
      .orElse(null);
    gasto.setCategoria(categoriaSugerida);

    Map<String, Object> model = new ModelMap();
    model.put(CLAVE_TITULO, "Revisar comprobante");
    model.put(CLAVE_SECCION, "registrar");
    model.put(CLAVE_URL_CANCELAR, RUTA_COMPROBANTE);
    model.put("gasto", gasto);
    model.put("categorias", categorias);
    model.put("datosComprobante", datos);
    model.put(
      "categoriaBajaConfianza",
      categoriaSugerida == null || datos.confianza().categoriaBajaConfianza()
    );
    model.put("fechaMinima", gastoServicio.obtenerFechaMinimaPermitida());
    model.put("fechaMaxima", gastoServicio.obtenerFechaMaximaPermitida());
    return new ModelAndView("revisar-comprobante", model);
  }

  // Se mira la firma del archivo, no la extensión ni el content-type que manda el navegador
  private String detectarTipoArchivo(byte[] archivo) {
    if (empiezaCon(archivo, FIRMA_PNG)) {
      return "image/png";
    }
    if (empiezaCon(archivo, FIRMA_JPEG)) {
      return "image/jpeg";
    }
    if (empiezaCon(archivo, FIRMA_PDF)) {
      return LectorComprobante.TIPO_PDF;
    }
    return null;
  }

  private boolean empiezaCon(byte[] contenido, byte[] firma) {
    if (contenido.length < firma.length) {
      return false;
    }
    for (int indice = 0; indice < firma.length; indice++) {
      if (contenido[indice] != firma[indice]) {
        return false;
      }
    }
    return true;
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
