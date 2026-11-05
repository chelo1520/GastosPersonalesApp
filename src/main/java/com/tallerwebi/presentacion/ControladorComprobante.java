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
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cargar gastos desde un comprobante: subir el archivo, revisar lo que se leyó y confirmar. */
@Controller
public class ControladorComprobante {

  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String RUTA_COMPROBANTE = "/registrar-gasto/comprobante";
  private static final String CLAVE_TITULO = "titulo";
  private static final String CLAVE_SECCION = "seccion";
  private static final String CLAVE_URL_CANCELAR = "urlCancelar";
  private static final String CLAVE_CONFIRMACION = "confirmacion";
  private static final String CLAVE_ERROR = "error";
  private static final byte[] FIRMA_PNG = { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
  private static final byte[] FIRMA_JPEG = { (byte) 0xff, (byte) 0xd8, (byte) 0xff };
  private static final byte[] FIRMA_PDF = { 0x25, 0x50, 0x44, 0x46 };
  private static final String FORMATOS_ACEPTADOS = "JPG, PNG o PDF";
  private static final long MAX_IMAGEN_BYTES = 5L * 1024 * 1024;

  private final GastoServicio gastoServicio;
  private final CategoriaRepositorio categoriaRepositorio;
  private final LectorComprobante lectorComprobante;

  @Autowired
  public ControladorComprobante(
    GastoServicio gastoServicio,
    CategoriaRepositorio categoriaRepositorio,
    LectorComprobante lectorComprobante
  ) {
    this.gastoServicio = gastoServicio;
    this.categoriaRepositorio = categoriaRepositorio;
    this.lectorComprobante = lectorComprobante;
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
      List<String> nombresCategorias = categorias.stream().map(Categoria::getNombre).toList();
      DatosComprobante datos = lectorComprobante.leer(contenido, tipoArchivo, nombresCategorias);
      return vistaRevision(ConfirmacionComprobante.de(datos, categorias), null);
    } catch (ComprobanteNoLegible | LectorComprobanteNoDisponible e) {
      return vistaCargaComprobante(e.getMessage());
    }
  }

  // Guarda los gastos que la persona dejó tildados en la revisión: todos o ninguno
  @RequestMapping(path = RUTA_COMPROBANTE + "/confirmar", method = RequestMethod.POST)
  public ModelAndView confirmarComprobante(
    @ModelAttribute(CLAVE_CONFIRMACION) ConfirmacionComprobante confirmacion,
    HttpServletRequest request,
    RedirectAttributes redirectAttributes
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    List<Gasto> gastos;
    try {
      gastos = gastosElegidos(confirmacion);
      gastoServicio.registrarGastos(gastos, usuario);
    } catch (GastoInvalidoExeption e) {
      return vistaRevision(confirmacion, e.getMessage());
    }

    // Sin alerta de repetición: los gastos de un mismo comprobante se dispararían entre sí
    Alerta alertaPresupuesto = gastoServicio.alertaDePresupuesto(gastos, usuario);
    if (alertaPresupuesto != null) {
      redirectAttributes.addFlashAttribute("alertaPresupuesto", alertaPresupuesto);
    }
    return new ModelAndView("redirect:/mostrar-gastos");
  }

  // Los tildados, cada uno con una categoría existente
  private List<Gasto> gastosElegidos(ConfirmacionComprobante confirmacion) {
    List<Gasto> gastos = new ArrayList<>();
    for (GastoPropuesto propuesto : confirmacion.getGastos()) {
      if (propuesto.isIncluir()) {
        Categoria categoria = propuesto.getCategoriaId() == null
          ? null
          : categoriaRepositorio.buscarPorId(propuesto.getCategoriaId());
        if (categoria == null) {
          throw new GastoInvalidoExeption(
            "Elegí una categoría para \"" + propuesto.getDescripcion() + "\"."
          );
        }
        gastos.add(propuesto.aGasto(categoria));
      }
    }
    if (gastos.isEmpty()) {
      throw new GastoInvalidoExeption("Elegí al menos un gasto para guardar.");
    }
    return gastos;
  }

  private ModelAndView vistaCargaComprobante(String error) {
    Map<String, Object> model = new ModelMap();
    model.put(CLAVE_TITULO, "Desde comprobante");
    model.put(CLAVE_SECCION, "registrar");
    model.put(CLAVE_URL_CANCELAR, "/registrar-gasto");
    if (error != null) {
      model.put(CLAVE_ERROR, error);
    }
    return new ModelAndView("cargar-comprobante", model);
  }

  private ModelAndView vistaRevision(ConfirmacionComprobante confirmacion, String error) {
    Map<String, Object> model = new ModelMap();
    model.put(CLAVE_TITULO, "Revisar comprobante");
    model.put(CLAVE_SECCION, "registrar");
    model.put(CLAVE_URL_CANCELAR, RUTA_COMPROBANTE);
    model.put(CLAVE_CONFIRMACION, confirmacion);
    model.put("categorias", categoriaRepositorio.obtenerTodas());
    model.put("fechaMinima", gastoServicio.obtenerFechaMinimaPermitida());
    model.put("fechaMaxima", gastoServicio.obtenerFechaMaximaPermitida());
    if (error != null) {
      model.put(CLAVE_ERROR, error);
    }
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
}
