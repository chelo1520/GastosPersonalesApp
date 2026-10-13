package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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

  private CategoriaRepositorio categoriaRepositorio;

  @Autowired
  public ControladorCategoria(CategoriaRepositorio categoriaRepositorio) {
    this.categoriaRepositorio = categoriaRepositorio;
  }

  // Botón "No es hormiga": la categoría pasa a esencial y sus gastos dejan de contarse
  @RequestMapping(path = "/categorias/{id}/no-es-hormiga", method = RequestMethod.POST)
  public ModelAndView marcarComoNoHormiga(
    @PathVariable("id") Long id,
    @RequestParam(value = "volver", required = false) String volver,
    HttpServletRequest request
  ) {
    return marcarEsencial(id, true, volver, request);
  }

  // Botón "Es hormiga": la categoría pasa a prescindible y sus gastos chicos y frecuentes se cuentan
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
    if (SesionUsuario.obtener(request) == null) {
      return new ModelAndView("redirect:/login");
    }

    // Una categoría fija es siempre esencial: no se cambia aunque llegue el pedido
    Categoria categoria = categoriaRepositorio.buscarPorId(id);
    if (categoria != null && !categoria.isFija()) {
      categoria.setEsencial(esencial);
      categoriaRepositorio.modificar(categoria);
    }

    String destino = PANTALLAS_DE_VUELTA.contains(volver) ? volver : "/home";
    return new ModelAndView("redirect:" + destino);
  }
}
