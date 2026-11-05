package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.ServicioCategoria;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * ControladorCategoria
 */
@Controller
public class ControladorCategoria {

  // Solo se vuelve a pantallas propias: evita redirigir a cualquier URL que llegue en el parámetro
  private static final List<String> PANTALLAS_DE_VUELTA = List.of("/home", "/mostrar-gastos");
  // El resumen de un mes puntual (check "¿Es hormiga?" de la tabla): se vuelve al mismo mes
  private static final Pattern RESUMEN_DE_UN_MES = Pattern.compile("/home\\?mes=\\d{4}-\\d{2}");

  private final ServicioCategoria servicioCategoria;

  @Autowired
  public ControladorCategoria(ServicioCategoria servicioCategoria) {
    this.servicioCategoria = servicioCategoria;
  }

  // Botón "Quitar categoría de hormiga": para este usuario la categoría pasa a esencial y sus
  // gastos dejan de contarse. La marca es de cada usuario, no de la categoría
  @RequestMapping(path = "/categorias/{id}/no-es-hormiga", method = RequestMethod.POST)
  public ModelAndView marcarComoNoHormiga(
    @PathVariable("id") Long id,
    @RequestParam(value = "volver", required = false) String volver,
    HttpServletRequest request
  ) {
    return marcarEsencial(id, true, volver, request);
  }

  // Botón "Marcar categoría como hormiga": la categoría pasa a prescindible y sus gastos chicos y frecuentes se cuentan
  @RequestMapping(path = "/categorias/{id}/es-hormiga", method = RequestMethod.POST)
  public ModelAndView marcarComoHormiga(
    @PathVariable("id") Long id,
    @RequestParam(value = "volver", required = false) String volver,
    HttpServletRequest request
  ) {
    return marcarEsencial(id, false, volver, request);
  }

  private ModelAndView marcarEsencial(
    Long id,
    boolean esencial,
    String volver,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView("redirect:/login");
    }

    if (esencial) {
      servicioCategoria.quitarDeHormiga(usuario, id);
    } else {
      servicioCategoria.marcarComoHormiga(usuario, id);
    }

    boolean vueltaValida =
      volver != null &&
      (PANTALLAS_DE_VUELTA.contains(volver) || RESUMEN_DE_UN_MES.matcher(volver).matches());
    String destino = vueltaValida ? volver : "/home";
    return new ModelAndView("redirect:" + destino);
  }
}
