package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
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

  private CategoriaRepositorio categoriaRepositorio;

  @Autowired
  public ControladorCategoria(CategoriaRepositorio categoriaRepositorio) {
    this.categoriaRepositorio = categoriaRepositorio;
  }

  // Botón "Quitar categoría de hormiga": la categoría pasa a esencial y sus gastos dejan de contarse
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
    if (SesionUsuario.obtener(request) == null) {
      return new ModelAndView("redirect:/login");
    }

    // Una categoría fija es siempre esencial: no se cambia aunque llegue el pedido
    Categoria categoria = categoriaRepositorio.buscarPorId(id);
    if (categoria != null && !categoria.isFija()) {
      categoria.setEsencial(esencial);
      categoriaRepositorio.modificar(categoria);
    }

    boolean vueltaValida =
      PANTALLAS_DE_VUELTA.contains(volver) ||
      (volver != null && RESUMEN_DE_UN_MES.matcher(volver).matches());
    String destino = vueltaValida ? volver : "/home";
    return new ModelAndView("redirect:" + destino);
  }
}
