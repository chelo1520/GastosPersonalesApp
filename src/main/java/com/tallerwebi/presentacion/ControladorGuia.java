package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioGuia;
import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Navegación del recorrido inicial, sin impedir el uso normal de la aplicación. */
@Controller
public class ControladorGuia {

  private static final int FINALIZADO = 5;
  private static final int RESUMEN = 4;
  // El valor 5 ya identifica guías finalizadas antes de agregar estos pasos.
  private static final int SIMULACION = 6;
  private static final int ASISTENTE = 7;
  private static final String POSPONER = "pausar";
  private final ServicioGuia guia;

  public ControladorGuia(ServicioGuia guia) {
    this.guia = guia;
  }

  @PostMapping("/guia/{accion}")
  public String cambiar(
    @PathVariable("accion") String accion,
    HttpServletRequest request,
    RedirectAttributes mensajes
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return "redirect:/login";
    }
    Integer actual = usuario.getGuiaPaso();
    int paso = actual == null ? 0 : actual;
    if (POSPONER.equals(accion)) {
      validarRango(paso, 0, 0);
      request.getSession(false).setAttribute("GUIA_POSPUESTA", true);
      return "redirect:/home";
    }
    paso = siguientePaso(accion, paso);
    guia.guardarPaso(usuario, paso);
    if (paso == FINALIZADO) {
      mensajes.addFlashAttribute("guiaFinalizada", true);
    }
    return "redirect:" + destino(paso);
  }

  private int siguientePaso(String accion, int paso) {
    switch (accion) {
      case "anterior":
        validarPasoActivo(paso, 1);
        if (paso == SIMULACION) {
          return RESUMEN;
        }
        return paso - 1;
      case "continuar":
        validarPasoActivo(paso, 0);
        if (paso == RESUMEN) {
          return SIMULACION;
        }
        if (paso == ASISTENTE) {
          return FINALIZADO;
        }
        return paso + 1;
      default:
        throw invalida();
    }
  }

  private void validarPasoActivo(int paso, int minimo) {
    validarRango(paso, minimo, ASISTENTE);
    if (paso == FINALIZADO) {
      throw invalida();
    }
  }

  private void validarRango(int paso, int minimo, int maximo) {
    if (paso < minimo || paso > maximo) {
      throw invalida();
    }
  }

  private String destino(int paso) {
    switch (paso) {
      case 1:
        return "/perfil";
      case 2:
        return "/presupuestos";
      case 3:
        return "/registrar-gasto";
      case SIMULACION:
        return "/simulacion";
      case ASISTENTE:
        return "/asistente";
      default:
        return "/home";
    }
  }

  private ResponseStatusException invalida() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acción de guía inválida");
  }
}
