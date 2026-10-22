package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ReglasDeContrasena;
import com.tallerwebi.dominio.ServicioCuenta;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.ContrasenaIncorrecta;
import com.tallerwebi.dominio.excepcion.ContrasenaInvalida;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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

  @Autowired
  public ControladorPerfil(ServicioCuenta servicioCuenta) {
    this.servicioCuenta = servicioCuenta;
  }

  @RequestMapping(path = "/perfil", method = RequestMethod.GET)
  public ModelAndView mostrarPerfil(HttpServletRequest request) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    return new ModelAndView(VISTA, USUARIO, usuario);
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

  private ModelAndView conError(Usuario usuario, String clave, String error) {
    Map<String, Object> model = new ModelMap();
    model.put(USUARIO, usuario);
    model.put(clave, error);
    return new ModelAndView(VISTA, model);
  }
}
