package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * ControladorResumen
 */
@Controller
public class ControladorResumen {

  private ServicioResumen servicioResumen;
  private Clock reloj;

  @Autowired
  public ControladorResumen(ServicioResumen servicioResumen) {
    this(servicioResumen, Clock.systemDefaultZone());
  }

  // Permite fijar "hoy" en los tests
  public ControladorResumen(ServicioResumen servicioResumen, Clock reloj) {
    this.servicioResumen = servicioResumen;
    this.reloj = reloj;
  }

  // mes: opcional, con formato yyyy-MM (ej. 2026-09). Sin mes, o con uno inválido, muestra el actual
  @RequestMapping(path = "/home", method = RequestMethod.GET)
  public ModelAndView mostrarResumen(
    @RequestParam(value = "mes", required = false) String mes,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView("redirect:/login");
    }

    YearMonth mesMostrado = elegirMes(mes);
    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, mesMostrado);

    Map<String, Object> model = new ModelMap();
    model.put("mesActual", NombreDeMes.de(mesMostrado));
    model.put("mesAnterior", mesMostrado.minusMonths(1).toString());
    model.put("mesSiguiente", mesMostrado.plusMonths(1).toString());
    model.put("gastado", resumen.getGastado());
    model.put("presupuesto", resumen.getPresupuesto());
    model.put("disponible", resumen.getDisponible());
    model.put("porcentajeUsado", resumen.getPorcentajeUsado());
    model.put("categorias", resumen.getCategorias());

    return new ModelAndView("resumen", model);
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
