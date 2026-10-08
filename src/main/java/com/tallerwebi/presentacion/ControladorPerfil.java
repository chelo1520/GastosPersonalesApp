package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * ControladorPerfil
 */
@Controller
public class ControladorPerfil {

  @RequestMapping(path = "/perfil", method = RequestMethod.GET)
  public ModelAndView mostrarPerfil(HttpServletRequest request) {
    HttpSession session = request.getSession(false);

    if (session == null || session.getAttribute("USUARIO") == null) {
      return new ModelAndView("redirect:/login");
    }

    Usuario usuario = (Usuario) session.getAttribute("USUARIO");
    return new ModelAndView("perfil", "usuario", usuario);
  }
}
