package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.ServicioGastoReal;
import com.tallerwebi.dominio.asistente.Veredicto;
import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * ControladorAsistente: "Tu gasto real", inflación y dólar sobre el historial del usuario
 */
@Controller
public class ControladorAsistente {

  static final int PERIODO_POR_DEFECTO = ServicioGastoReal.PERIODO_POR_DEFECTO;
  static final List<Integer> PERIODOS = List.of(3, 6, 12);
  // Por debajo de medio punto se dice "lo mismo"
  private static final double DIFERENCIA_MINIMA = 0.5;

  private final ServicioGastoReal servicioGastoReal;

  @Autowired
  public ControladorAsistente(ServicioGastoReal servicioGastoReal) {
    this.servicioGastoReal = servicioGastoReal;
  }

  // periodo: cantidad de meses (3, 6 o 12); cualquier otro valor cae en 6
  @RequestMapping(path = "/asistente", method = RequestMethod.GET)
  public ModelAndView mostrarAsistente(
    @RequestParam(value = "periodo", required = false) Integer periodo,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView("redirect:/login");
    }

    int meses = periodo != null && PERIODOS.contains(periodo) ? periodo : PERIODO_POR_DEFECTO;
    Map<String, Object> model = new ModelMap();
    model.put("periodo", meses);
    model.put("periodos", PERIODOS);

    try {
      GastoReal gastoReal = servicioGastoReal.calcular(usuario, meses);
      model.put("gastoReal", gastoReal);
      model.put("meses", gastoReal.getMeses().stream().map(MesAsistente::new).toList());
      model.put("veredictoTexto", textoDelVeredicto(gastoReal.getVeredicto()));
      if (gastoReal.getUltimoMesCerrado() != null) {
        model.put("ultimoMesCerrado", NombreDeMes.de(gastoReal.getUltimoMesCerrado()));
      }
    } catch (IndicadoresNoDisponibles e) {
      // Sin IPC ni dólar no hay análisis posible: la vista avisa y no muestra el resto
      model.put("indicadoresNoDisponibles", true);
    }

    return new ModelAndView("asistente", model);
  }

  // "Desde Mayo 2026 tus gastos subieron 25 %. La inflación fue 21 %: gastás 3,5 % más en serio."
  static String textoDelVeredicto(Veredicto veredicto) {
    if (veredicto == null) {
      return null;
    }
    double real = veredicto.getVariacionReal();
    String cierre;
    if (Math.abs(real) < DIFERENCIA_MINIMA) {
      cierre = "gastás lo mismo en serio.";
    } else if (real > 0) {
      cierre = "gastás " + porcentaje(real) + " % más en serio.";
    } else {
      cierre = "gastás " + porcentaje(-real) + " % menos en serio.";
    }
    double nominal = veredicto.getVariacionNominal();
    String movimiento = nominal >= 0
      ? "subieron " + porcentaje(nominal) + " %"
      : "bajaron " + porcentaje(-nominal) + " %";
    return (
      "Desde " +
      NombreDeMes.de(veredicto.getDesde()) +
      " tus gastos " +
      movimiento +
      ". La inflación fue " +
      porcentaje(veredicto.getInflacionAcumulada()) +
      " %: " +
      cierre
    );
  }

  private static String porcentaje(double valor) {
    return String.format(java.util.Locale.of("es", "AR"), "%.1f", valor);
  }
}
