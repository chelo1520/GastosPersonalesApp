package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ReglasDeContrasena;
import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorLogin {

  private static final String VISTA_REGISTRO = "nuevo-usuario";

  private ServicioLogin servicioLogin;

  @Autowired
  public ControladorLogin(ServicioLogin servicioLogin) {
    this.servicioLogin = servicioLogin;
  }

  @RequestMapping("/login")
  public ModelAndView irALogin() {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("datosLogin", new DatosLogin());
    return new ModelAndView("login", modelo);
  }

  @RequestMapping(path = "/validar-login", method = RequestMethod.POST)
  public ModelAndView validarLogin(
    @ModelAttribute("datosLogin") DatosLogin datosLogin,
    HttpServletRequest request
  ) {
    Usuario usuarioBuscado = servicioLogin.consultarUsuario(
      datosLogin.getEmail(),
      datosLogin.getPassword()
    );
    if (usuarioBuscado != null) {
      request.getSession().setAttribute("USUARIO", usuarioBuscado);
      request.getSession().setAttribute("ROL", usuarioBuscado.getRol());
      return new ModelAndView("redirect:/home");
    } else {
      Map<String, Object> model = new ModelMap();
      model.put("error", "Usuario o clave incorrecta");
      return new ModelAndView("login", model);
    }
  }

  @RequestMapping(path = "/registrarme", method = RequestMethod.POST)
  public ModelAndView registrarme(@ModelAttribute("usuario") Usuario usuario) {
    // El formulario ya lo valida, pero el pedido puede llegar sin pasar por él
    String errorDeDatos = validarDatosDeRegistro(usuario);
    if (errorDeDatos != null) {
      return volverAlRegistro(errorDeDatos);
    }
    try {
      servicioLogin.registrar(usuario);
    } catch (UsuarioExistente e) {
      return volverAlRegistro("El usuario ya existe");
    } catch (Exception e) {
      return volverAlRegistro("Error al registrar el nuevo usuario");
    }
    // El login muestra "Tu cuenta ya está creada" con este parámetro
    return new ModelAndView("redirect:/login?registrado");
  }

  private ModelAndView volverAlRegistro(String error) {
    Map<String, Object> model = new ModelMap();
    model.put("error", error);
    return new ModelAndView(VISTA_REGISTRO, model);
  }

  private String validarDatosDeRegistro(Usuario usuario) {
    if (usuario.getEmail() == null || !usuario.getEmail().contains("@")) {
      return "Ingresá un email válido";
    }
    if (!ReglasDeContrasena.cumple(usuario.getPassword())) {
      return ReglasDeContrasena.MENSAJE;
    }
    return null;
  }

  @RequestMapping(path = "/nuevo-usuario", method = RequestMethod.GET)
  public ModelAndView nuevoUsuario() {
    Map<String, Object> model = new ModelMap();
    model.put("usuario", new Usuario());
    return new ModelAndView(VISTA_REGISTRO, model);
  }

  @RequestMapping(path = "/", method = RequestMethod.GET)
  public ModelAndView inicio() {
    return new ModelAndView("redirect:/login");
  }
}
