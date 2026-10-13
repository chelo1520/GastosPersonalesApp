package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.CategoriaRepositorio;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
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
 * ControladorPresupuesto
 */
@Controller
public class ControladorPresupuesto {

  private ServicioPresupuesto servicioPresupuesto;
  private ServicioResumen servicioResumen;
  private CategoriaRepositorio categoriaRepositorio;
  private Clock reloj;

  @Autowired
  public ControladorPresupuesto(
    ServicioPresupuesto servicioPresupuesto,
    ServicioResumen servicioResumen,
    CategoriaRepositorio categoriaRepositorio
  ) {
    this(servicioPresupuesto, servicioResumen, categoriaRepositorio, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public ControladorPresupuesto(
    ServicioPresupuesto servicioPresupuesto,
    ServicioResumen servicioResumen,
    CategoriaRepositorio categoriaRepositorio,
    Clock reloj
  ) {
    this.servicioPresupuesto = servicioPresupuesto;
    this.servicioResumen = servicioResumen;
    this.categoriaRepositorio = categoriaRepositorio;
    this.reloj = reloj;
  }

  // mes: opcional, con formato yyyy-MM. Sin mes, o con uno inválido, muestra el actual
  @RequestMapping(path = "/presupuestos", method = RequestMethod.GET)
  public ModelAndView mostrarPresupuestos(
    @RequestParam(value = "mes", required = false) String mes,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView("redirect:/login");
    }

    YearMonth mesMostrado = elegirMes(mes);
    return vistaPresupuestos(usuario, mesMostrado, new DatosPresupuesto(), null);
  }

  @RequestMapping(path = "/presupuestos", method = RequestMethod.POST)
  public ModelAndView definirPresupuesto(
    @ModelAttribute("datosPresupuesto") DatosPresupuesto datos,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView("redirect:/login");
    }

    YearMonth mes = elegirMes(datos.getMes());
    Categoria categoria = categoriaRepositorio.buscarPorId(datos.getCategoriaId());

    try {
      servicioPresupuesto.definir(usuario, categoria, mes, datos.getMonto());
    } catch (PresupuestoInvalido e) {
      return vistaPresupuestos(usuario, mes, datos, e.getMessage());
    }

    return new ModelAndView("redirect:/presupuestos?mes=" + mes);
  }

  private ModelAndView vistaPresupuestos(
    Usuario usuario,
    YearMonth mes,
    DatosPresupuesto datos,
    String error
  ) {
    List<Categoria> categorias = categoriaRepositorio.obtenerTodas();
    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, mes);

    // Todas las categorías en el orden del maestro, con lo gastado y el presupuesto del mes;
    // al final, los gastos sin categoría si los hubiera
    Map<String, ResumenCategoria> porNombre = new HashMap<>();
    for (ResumenCategoria categoria : resumen.getCategorias()) {
      porNombre.put(categoria.getNombre(), categoria);
    }

    List<ResumenCategoria> filas = new ArrayList<>();
    for (Categoria categoria : categorias) {
      ResumenCategoria fila = porNombre.remove(categoria.getNombre());
      filas.add(fila != null ? fila : new ResumenCategoria(categoria.getNombre(), 0.0, 0.0));
    }
    filas.addAll(porNombre.values());

    datos.setMes(mes.toString());

    Map<String, Object> model = new ModelMap();
    model.put("mesActual", NombreDeMes.de(mes));
    model.put("mes", mes.toString());
    model.put("mesAnterior", mes.minusMonths(1).toString());
    model.put("mesSiguiente", mes.plusMonths(1).toString());
    model.put("filas", filas);
    model.put("totalPresupuestado", resumen.getPresupuesto());
    model.put("categorias", categorias);
    model.put("datosPresupuesto", datos);
    if (error != null) {
      model.put("error", error);
    }

    return new ModelAndView("presupuestos", model);
  }

  private YearMonth elegirMes(String mes) {
    if (mes == null || mes.isBlank()) {
      return YearMonth.now(reloj);
    }
    try {
      return YearMonth.parse(mes);
    } catch (DateTimeParseException e) {
      return YearMonth.now(reloj);
    }
  }
}
