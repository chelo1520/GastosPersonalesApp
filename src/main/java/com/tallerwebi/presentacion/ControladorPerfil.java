package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ReglasDeContrasena;
import com.tallerwebi.dominio.ServicioCuenta;
import com.tallerwebi.dominio.ServicioGuia;
import com.tallerwebi.dominio.ServicioPerfil;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.ContrasenaIncorrecta;
import com.tallerwebi.dominio.excepcion.ContrasenaInvalida;
import com.tallerwebi.dominio.excepcion.DatosPersonalesInvalidos;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import com.tallerwebi.dominio.presupuesto.MesDePresupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.servlet.ModelAndView;

/**
 * ControladorPerfil: datos de la cuenta, cambiar la contraseña y eliminar la cuenta
 */
@Controller
public class ControladorPerfil {

  private static final String VISTA = "perfil";
  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String USUARIO = "usuario";

  private final ServicioCuenta servicioCuenta;
  private final ServicioPerfil servicioPerfil;
  private final ServicioPresupuesto servicioPresupuesto;
  private final ServicioGuia servicioGuia;

  @Autowired
  public ControladorPerfil(
    ServicioCuenta servicioCuenta,
    ServicioPerfil servicioPerfil,
    ServicioPresupuesto servicioPresupuesto,
    ServicioGuia servicioGuia
  ) {
    this.servicioCuenta = servicioCuenta;
    this.servicioPerfil = servicioPerfil;
    this.servicioPresupuesto = servicioPresupuesto;
    this.servicioGuia = servicioGuia;
  }

  // anio: el de la tabla anual de presupuestos; mes: se acepta por compatibilidad y fija el año
  @RequestMapping(path = "/perfil", method = RequestMethod.GET)
  public ModelAndView mostrarPerfil(
    @RequestParam(value = "mes", required = false) String mes,
    @RequestParam(value = "anio", required = false) Integer anio,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    YearMonth elegido = anio != null ? YearMonth.of(anio, 1) : elegirMes(mes);
    return new ModelAndView(VISTA, conMes(usuario, elegido));
  }

  @RequestMapping(path = "/perfil/presupuesto/mes", method = RequestMethod.POST)
  public ModelAndView definirPresupuestoDelMes(
    @RequestParam(value = "mes", required = false) String mes,
    @RequestParam(value = "monto", required = false) Double monto,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    YearMonth elegido = elegirMes(mes);
    try {
      servicioPresupuesto.definirPresupuestoDelMes(usuario, elegido, monto);
    } catch (PresupuestoInvalido e) {
      Map<String, Object> model = conMes(usuario, elegido);
      model.put("errorMes", e.getMessage());
      return new ModelAndView(VISTA, model);
    }
    // Al resumen de ese mes: ahí se ve el efecto del presupuesto (devolución del 2026-11-05)
    return new ModelAndView("redirect:/home?mes=" + elegido + "&presupuestoGuardado");
  }

  // Sin mes, o con uno inválido, usa el actual
  private YearMonth elegirMes(String mes) {
    if (mes == null) {
      return YearMonth.now();
    }
    try {
      return YearMonth.parse(mes);
    } catch (DateTimeParseException e) {
      return YearMonth.now();
    }
  }

  // La tabla anual muestra el año del mes elegido; los meses se arman con el total y lo asignado
  private Map<String, Object> conMes(Usuario usuario, YearMonth mes) {
    Map<String, Object> model = new ModelMap();
    model.put(USUARIO, usuario);
    model.put("mesPresupuesto", mes.toString());
    model.put("totalDelMes", servicioPresupuesto.presupuestoDelMes(usuario, mes));
    YearMonth mesActual = YearMonth.now();
    List<MesDelAnio> meses = new ArrayList<>();
    for (MesDePresupuesto datos : servicioPresupuesto.resumenDelAnio(usuario, mes.getYear())) {
      meses.add(new MesDelAnio(datos, mesActual));
    }
    model.put("anio", mes.getYear());
    model.put("anioAnterior", mes.getYear() - 1);
    model.put("anioSiguiente", mes.getYear() + 1);
    model.put("mesesDelAnio", meses);
    return model;
  }

  // Datos personales: opcionales, se pueden dejar vacíos. Al guardar, el perfil muestra el aviso
  @RequestMapping(path = "/perfil/datos", method = RequestMethod.POST)
  public ModelAndView actualizarDatosPersonales(
    @ModelAttribute("datosPersonales") DatosPersonales datos,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    LocalDate fechaNacimiento;
    try {
      fechaNacimiento = fechaDesde(datos.getFechaNacimiento());
    } catch (DateTimeParseException e) {
      return conError(usuario, "errorDatos", "La fecha de nacimiento no es válida");
    }
    try {
      servicioPerfil.actualizarDatosPersonales(
        usuario,
        datos.getNombre(),
        datos.getApellido(),
        fechaNacimiento
      );
    } catch (DatosPersonalesInvalidos e) {
      return conError(usuario, "errorDatos", e.getMessage());
    }
    return new ModelAndView("redirect:/perfil?datosGuardados");
  }

  private static LocalDate fechaDesde(String texto) {
    return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.trim());
  }

  @RequestMapping(path = "/perfil/contrasena", method = RequestMethod.POST)
  public ModelAndView cambiarContrasena(
    @RequestParam(value = "actual", required = false) String actual,
    @RequestParam(value = "password", required = false) String password,
    @RequestParam(value = "confirmacion", required = false) String confirmacion,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    // El formulario ya lo valida, pero el pedido puede llegar sin pasar por él
    if (password == null || !password.equals(confirmacion)) {
      return conError(usuario, "errorContrasena", "Las contraseñas nuevas no coinciden");
    }
    try {
      Usuario actualizado = servicioCuenta.cambiarContrasena(usuario, actual, password);
      request.getSession(false).setAttribute("USUARIO", actualizado);
    } catch (ContrasenaIncorrecta e) {
      return conError(usuario, "errorContrasena", "La contraseña actual no es correcta");
    } catch (ContrasenaInvalida e) {
      return conError(usuario, "errorContrasena", ReglasDeContrasena.MENSAJE);
    }
    // El perfil muestra "Tu contraseña se cambió" con este parámetro
    return new ModelAndView("redirect:/perfil?contrasenaCambiada");
  }

  @RequestMapping(path = "/perfil/eliminar", method = RequestMethod.POST)
  public ModelAndView eliminarCuenta(
    @RequestParam(value = "password", required = false) String password,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    try {
      servicioCuenta.eliminarCuenta(usuario, password);
    } catch (ContrasenaIncorrecta e) {
      return conError(
        usuario,
        "errorEliminar",
        "La contraseña no es correcta: la cuenta no se eliminó"
      );
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    // El login muestra "Tu cuenta se eliminó" con este parámetro
    return new ModelAndView("redirect:/login?cuentaEliminada");
  }

  @RequestMapping(path = "/perfil/presupuesto", method = RequestMethod.POST)
  public ModelAndView definirPresupuesto(
    @RequestParam(value = "ingresoMensual", required = false) Double ingresoMensual,
    @RequestParam(value = "porcentaje", required = false) Double porcentaje,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    try {
      servicioPerfil.definirPresupuesto(usuario, ingresoMensual, porcentaje);
    } catch (PresupuestoInvalido e) {
      return conError(usuario, "error", e.getMessage());
    }
    if (Integer.valueOf(1).equals(usuario.getGuiaPaso())) {
      servicioGuia.guardarPaso(usuario, 2);
      return new ModelAndView("redirect:/presupuestos");
    }
    // Al resumen, donde se ve el presupuesto recién definido (devolución del 2026-11-05)
    return new ModelAndView("redirect:/home?presupuestoGuardado");
  }

  private ModelAndView conError(Usuario usuario, String clave, String error) {
    Map<String, Object> model = conMes(usuario, YearMonth.now());
    model.put(clave, error);
    return new ModelAndView(VISTA, model);
  }
}
