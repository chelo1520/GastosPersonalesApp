package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.ServicioGastoReal;
import com.tallerwebi.dominio.asistente.Veredicto;
import com.tallerwebi.dominio.consejo.Consejo;
import com.tallerwebi.dominio.consejo.ServicioConsejos;
import com.tallerwebi.dominio.excepcion.ConsejoNoEncontrado;
import com.tallerwebi.dominio.excepcion.IndicadoresNoDisponibles;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * ControladorAsistente: "Tu gasto real", inflación y dólar sobre el historial del usuario
 */
@Controller
public class ControladorAsistente {

  static final int PERIODO_POR_DEFECTO = ServicioGastoReal.PERIODO_POR_DEFECTO;
  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String PARAM_PERIODO = "periodo";
  // Parámetro con el que el asistente avisa que la acción no se pudo hacer
  static final String AVISO_CONSEJO_INEXISTENTE = "consejo-inexistente";
  static final List<Integer> PERIODOS = List.of(3, 6, 12);

  private final ServicioGastoReal servicioGastoReal;
  private final ServicioConsejos servicioConsejos;
  private final ServicioPresupuesto servicioPresupuesto;

  @Autowired
  public ControladorAsistente(
    ServicioGastoReal servicioGastoReal,
    ServicioConsejos servicioConsejos,
    ServicioPresupuesto servicioPresupuesto
  ) {
    this.servicioGastoReal = servicioGastoReal;
    this.servicioConsejos = servicioConsejos;
    this.servicioPresupuesto = servicioPresupuesto;
  }

  // periodo: cantidad de meses (3, 6 o 12); cualquier otro valor cae en 6
  @RequestMapping(path = "/asistente", method = RequestMethod.GET)
  public ModelAndView mostrarAsistente(
    @RequestParam(value = PARAM_PERIODO, required = false) Integer periodo,
    @RequestParam(value = "aviso", required = false) String aviso,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }

    int meses = periodo != null && PERIODOS.contains(periodo) ? periodo : PERIODO_POR_DEFECTO;
    Map<String, Object> model = new ModelMap();
    model.put(PARAM_PERIODO, meses);
    model.put("periodos", PERIODOS);
    if (AVISO_CONSEJO_INEXISTENTE.equals(aviso)) {
      model.put("aviso", "Ese consejo ya no está disponible.");
    }

    try {
      GastoReal gastoReal = servicioGastoReal.calcular(usuario, meses);
      model.put("gastoReal", gastoReal);
      model.put("meses", gastoReal.getMeses().stream().map(MesAsistente::new).toList());
      if (gastoReal.getUltimoMesCerrado() != null) {
        model.put("ultimoMesCerrado", NombreDeMes.de(gastoReal.getUltimoMesCerrado()));
        model.put(
          "mesAnteriorAlCerrado",
          NombreDeMes.de(gastoReal.getUltimoMesCerrado().minusMonths(1)).replaceAll(" \\d{4}$", "")
        );
      }
      // La tabla muestra primero lo que más cambió en términos reales, que es lo que señala el asistente
      model.put("categorias", porMagnitudDelCambioReal(gastoReal.getCategorias()));
      if (gastoReal.getVeredicto() != null) {
        model.put("periodoTexto", textoDelPeriodo(gastoReal.getVeredicto()));
      }
      // Los consejos (y el veredicto) salen del mismo análisis que muestra la pantalla
      List<Consejo> consejos = servicioConsejos.obtenerPendientes(usuario, gastoReal);
      model.put("consejos", consejos);
      // La síntesis de la IA tarda: la vista la pide en segundo plano a /asistente/sintesis
      model.put("sintesisPendiente", servicioConsejos.faltaLaSintesis(consejos));
      // La propuesta muestra el monto vigente tachado: es dato vivo, no del consejo
      YearMonth mesActual = YearMonth.now();
      model.put("montosActuales", montosDelMes(usuario, mesActual));
      model.put("mesActualNombre", NombreDeMes.de(mesActual));
    } catch (IndicadoresNoDisponibles e) {
      // Sin IPC ni dólar no hay análisis posible: la vista avisa y no muestra el resto
      model.put("indicadoresNoDisponibles", true);
    }

    return new ModelAndView("asistente", model);
  }

  // Mayor cambio real primero, suba o baja; las que no se pueden comparar (nuevas, sin datos) al final
  static List<CategoriaGastoReal> porMagnitudDelCambioReal(List<CategoriaGastoReal> categorias) {
    List<CategoriaGastoReal> ordenadas = new ArrayList<>(categorias);
    ordenadas.sort(
      Comparator
        .comparing((CategoriaGastoReal c) -> c.getVariacionReal() == null)
        .thenComparing(c -> -Math.abs(c.getVariacionReal() == null ? 0 : c.getVariacionReal()))
    );
    return ordenadas;
  }

  // "Mayo a Septiembre 2026": el primer y el último mes cerrado que compara el veredicto
  static String textoDelPeriodo(Veredicto veredicto) {
    String desde = NombreDeMes.de(veredicto.getDesde());
    String hasta = NombreDeMes.de(veredicto.getHasta());
    if (veredicto.getDesde().getYear() == veredicto.getHasta().getYear()) {
      desde = desde.substring(0, desde.length() - 5);
    }
    return desde + " a " + hasta;
  }

  private Map<String, Double> montosDelMes(Usuario usuario, YearMonth mes) {
    Map<String, Double> montos = new HashMap<>();
    for (Presupuesto presupuesto : servicioPresupuesto.obtenerDelMes(usuario, mes)) {
      montos.put(presupuesto.getCategoria().getNombre(), presupuesto.getMonto());
    }
    return montos;
  }

  // La síntesis del período como texto plano; 204 si no hay (sin clave, sin veredicto, sin red)
  @RequestMapping(
    path = "/asistente/sintesis",
    method = RequestMethod.GET,
    produces = "text/plain;charset=UTF-8"
  )
  @ResponseBody
  public ResponseEntity<String> sintesis(
    @RequestParam(value = PARAM_PERIODO, required = false) Integer periodo,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    int meses = periodo != null && PERIODOS.contains(periodo) ? periodo : PERIODO_POR_DEFECTO;
    try {
      String sintesis = servicioConsejos.sintetizar(
        usuario,
        servicioGastoReal.calcular(usuario, meses)
      );
      return sintesis == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(sintesis);
    } catch (IndicadoresNoDisponibles e) {
      return ResponseEntity.noContent().build();
    }
  }

  @RequestMapping(path = "/asistente/aplicar/{id}", method = RequestMethod.POST)
  public ModelAndView aplicarConsejo(
    @PathVariable("id") Long id,
    @RequestParam(value = PARAM_PERIODO, required = false) Integer periodo,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    try {
      servicioConsejos.aplicar(id, usuario);
    } catch (ConsejoNoEncontrado e) {
      // Ya no existe o es de otro usuario: se vuelve al asistente con un aviso
      return new ModelAndView(volverAlAsistente(periodo) + "&aviso=" + AVISO_CONSEJO_INEXISTENTE);
    }
    return new ModelAndView(volverAlAsistente(periodo));
  }

  @RequestMapping(path = "/asistente/descartar/{id}", method = RequestMethod.POST)
  public ModelAndView descartarConsejo(
    @PathVariable("id") Long id,
    @RequestParam(value = PARAM_PERIODO, required = false) Integer periodo,
    HttpServletRequest request
  ) {
    Usuario usuario = SesionUsuario.obtener(request);
    if (usuario == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    try {
      servicioConsejos.descartar(id, usuario);
    } catch (ConsejoNoEncontrado e) {
      return new ModelAndView(volverAlAsistente(periodo) + "&aviso=" + AVISO_CONSEJO_INEXISTENTE);
    }
    return new ModelAndView(volverAlAsistente(periodo));
  }

  private static String volverAlAsistente(Integer periodo) {
    return "redirect:/asistente?periodo=" + (periodo != null ? periodo : PERIODO_POR_DEFECTO);
  }
}
