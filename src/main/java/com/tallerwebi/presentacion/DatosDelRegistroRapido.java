package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.categoria.ServicioCategoria;
import com.tallerwebi.dominio.gasto.GastoServicio;
import java.time.LocalDate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Lo que necesita el modal "Registrar gasto" (registro rápido, rediseño v2) en cada vista que tiene
 * el botón: las categorías y los límites de fecha, los mismos que la pantalla Registrar. Se agrega
 * una sola vez acá en lugar de repetirlo en cada controlador. En una redirección no viaja.
 */
@ControllerAdvice(
  assignableTypes = {
    ControladorResumen.class,
    ControladorGasto.class,
    ControladorPresupuesto.class,
    ControladorSimulacion.class,
  }
)
public class DatosDelRegistroRapido {

  private final ServicioCategoria servicioCategoria;
  private final GastoServicio gastoServicio;

  public DatosDelRegistroRapido(ServicioCategoria servicioCategoria, GastoServicio gastoServicio) {
    this.servicioCategoria = servicioCategoria;
    this.gastoServicio = gastoServicio;
  }

  @ModelAttribute
  public void agregar(Model model) {
    model.addAttribute("categoriasRegistro", servicioCategoria.obtenerTodas());
    model.addAttribute("fechaHoy", LocalDate.now());
    model.addAttribute("fechaMinimaRegistro", gastoServicio.obtenerFechaMinimaPermitida());
    model.addAttribute("fechaMaximaRegistro", gastoServicio.obtenerFechaMaximaPermitida());
  }
}
