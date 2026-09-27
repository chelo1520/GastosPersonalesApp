package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorGastos {

  private GastoServicio gastoServicio;

  @Autowired
  public ControladorGastos(GastoServicio gastoServicio) {
    this.gastoServicio = gastoServicio;
  }

  @RequestMapping(path = "/mostrar-gastos", method = RequestMethod.GET)
  public ModelAndView mostrarGastos(Usuario usuario) {

    List<Gasto> gastos = gastoServicio.obtenerGastos(usuario);

    Map<String, Object> model = new ModelMap();
    model.put("gastos", gastos);

    return new ModelAndView("mostrar-gastos", model);
  }

  @RequestMapping(path = "/registrar-gasto", method = RequestMethod.POST)
  public ModelAndView registrarGasto(Gasto gasto, Usuario usuario) {

    gastoServicio.registrarGasto(gasto, usuario);

    return new ModelAndView("redirect:/mostrar-gastos");
  }

  @RequestMapping(path = "/sumar-gastos", method = RequestMethod.GET)
  public ModelAndView sumarGastos(

          @RequestParam("usuario") Usuario usuario,
          @RequestParam("fecha") LocalDate desde,
          @RequestParam("fecha") LocalDate hasta) {

    Double total = gastoServicio.sumarGastos(usuario, desde, hasta);

    Map<String, Object> model = new ModelMap();
    model.put("total", total);

    return new ModelAndView("sumar-gastos", model);
  }
}
