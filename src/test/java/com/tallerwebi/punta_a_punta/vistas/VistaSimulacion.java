package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.List;

public class VistaSimulacion extends VistaWeb {

  private static final String FORM_EXTRAORDINARIO = "form[data-tipo='extraordinario']";
  private static final String FORM_ORDINARIO = "form[data-tipo='ordinario']";

  public VistaSimulacion(Page page) {
    super(page);
  }

  public String obtenerTitulo() {
    return this.obtenerTextoDelElemento("h1");
  }

  public boolean estaVisibleElAvisoDeMesVacio() {
    return page.locator("#sim-vacio").isVisible();
  }

  public void agregarGastoExtraordinario(String descripcion, String monto) {
    agregarGasto(FORM_EXTRAORDINARIO, descripcion, monto);
  }

  public void agregarGastoOrdinario(String descripcion, String monto) {
    agregarGasto(FORM_ORDINARIO, descripcion, monto);
  }

  public List<String> obtenerDescripcionesDeLaLista() {
    return page.locator("#sim-lista .app-sim-descripcion > span:first-child").allTextContents();
  }

  public String obtenerTagDelGasto(String descripcion) {
    return obtenerItem(descripcion).locator(".app-sim-tag").textContent();
  }

  public void desmarcarGasto(String descripcion) {
    obtenerItem(descripcion).locator(".app-sim-check").uncheck();
  }

  public String obtenerTotalSimulado() {
    return this.obtenerTextoDelElemento("#sim-total");
  }

  public String obtenerTextoDeLaDiferencia() {
    return this.obtenerTextoDelElemento("#sim-diferencia-label");
  }

  private void agregarGasto(String selectorFormulario, String descripcion, String monto) {
    this.escribirEnElElemento(selectorFormulario + " .app-sim-input-descripcion", descripcion);
    this.escribirEnElElemento(selectorFormulario + " .app-sim-input-monto", monto);
    this.darClickEnElElemento(selectorFormulario + " button[type='submit']");
  }

  private Locator obtenerItem(String descripcion) {
    return page.locator("#sim-lista .app-sim-item[data-descripcion='" + descripcion + "']");
  }
}
