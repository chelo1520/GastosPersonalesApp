package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;
import java.time.LocalDate;

public class VistaRegistrarGasto extends VistaWeb {

  public VistaRegistrarGasto(Page page) {
    super(page);
    page.navigate(URL_BASE + "/registrar-gasto");
  }

  public void registrarGasto(String importe, LocalDate fecha, String descripcion) {
    completarFormulario(importe, fecha, descripcion);
    darClickEnGuardar();
    page.waitForURL("**/mostrar-gastos**");
  }

  public void completarFormulario(String importe, LocalDate fecha, String descripcion) {
    this.escribirEnElElemento("#importe", importe);
    // El input type="date" no acepta tipeo: se completa con el valor ISO (yyyy-MM-dd)
    page.locator("#fecha").fill(fecha.toString());
    this.escribirEnElElemento("#descripcion", descripcion);
  }

  public void darClickEnGuardar() {
    this.darClickEnElElemento("#btn-guardar");
  }

  public boolean elImporteEsValido() {
    return (Boolean) page.locator("#importe").evaluate("input => input.checkValidity()");
  }

  // Con min/max en el input, el navegador marca inválida una fecha fuera del mes actual
  public boolean laFechaEsValida() {
    return (Boolean) page.locator("#fecha").evaluate("input => input.checkValidity()");
  }
}
