package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ReglasDeContrasena;
import com.tallerwebi.dominio.excepcion.TokenRecuperacionInvalido;
import com.tallerwebi.dominio.recuperacion.ServicioRecuperacion;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/** "¿Olvidaste tu contraseña?": pedir el mail y elegir la contraseña nueva desde su link. */
@Controller
public class ControladorRecuperacion {

  private static final String VISTA_OLVIDE = "olvide-contrasena";
  private static final String VISTA_RESTABLECER = "restablecer-contrasena";
  private static final String TOKEN = "token";
  private static final String ERROR = "error";

  private final ServicioRecuperacion servicioRecuperacion;

  @Autowired
  public ControladorRecuperacion(ServicioRecuperacion servicioRecuperacion) {
    this.servicioRecuperacion = servicioRecuperacion;
  }

  @RequestMapping(path = "/olvide-contrasena", method = RequestMethod.GET)
  public ModelAndView irAOlvideContrasena() {
    return new ModelAndView(VISTA_OLVIDE);
  }

  // Siempre la misma respuesta, exista o no la cuenta
  @RequestMapping(path = "/olvide-contrasena", method = RequestMethod.POST)
  public ModelAndView pedirMail(@RequestParam(value = "email", required = false) String email) {
    servicioRecuperacion.solicitar(email);
    Map<String, Object> model = new ModelMap();
    model.put("enviado", true);
    return new ModelAndView(VISTA_OLVIDE, model);
  }

  @RequestMapping(path = "/restablecer-contrasena", method = RequestMethod.GET)
  public ModelAndView irARestablecer(@RequestParam(value = TOKEN, required = false) String token) {
    Map<String, Object> model = new ModelMap();
    if (servicioRecuperacion.esValido(token)) {
      model.put(TOKEN, token);
    } else {
      model.put("linkInvalido", true);
    }
    return new ModelAndView(VISTA_RESTABLECER, model);
  }

  @RequestMapping(path = "/restablecer-contrasena", method = RequestMethod.POST)
  public ModelAndView restablecer(
    @RequestParam(value = TOKEN, required = false) String token,
    @RequestParam(value = "password", required = false) String password,
    @RequestParam(value = "confirmacion", required = false) String confirmacion
  ) {
    // El formulario ya lo valida, pero el pedido puede llegar sin pasar por él
    String error = validarPassword(password, confirmacion);
    if (error != null) {
      Map<String, Object> model = new ModelMap();
      model.put(TOKEN, token);
      model.put(ERROR, error);
      return new ModelAndView(VISTA_RESTABLECER, model);
    }
    try {
      servicioRecuperacion.restablecer(token, password);
    } catch (TokenRecuperacionInvalido e) {
      Map<String, Object> model = new ModelMap();
      model.put("linkInvalido", true);
      return new ModelAndView(VISTA_RESTABLECER, model);
    }
    // El login muestra "Tu contraseña se cambió" con este parámetro
    return new ModelAndView("redirect:/login?restablecida");
  }

  private String validarPassword(String password, String confirmacion) {
    if (!ReglasDeContrasena.cumple(password)) {
      return ReglasDeContrasena.MENSAJE;
    }
    if (!password.equals(confirmacion)) {
      return "Las contraseñas no coinciden";
    }
    return null;
  }
}
