package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;
import java.util.List;

public class VistaMisGastos extends VistaWeb {

  public VistaMisGastos(Page page) {
    super(page);
  }

  public List<String> obtenerFilasDeLaTabla() {
    return page
      .locator("table.app-gastos-tabla tbody tr")
      .allInnerTexts()
      .stream()
      .map(fila -> fila.replaceAll("\\s+", " ").trim())
      .toList();
  }

  public String obtenerTotal() {
    return this.obtenerTextoDelElemento(".app-total-card strong").replaceAll("\\s+", " ").trim();
  }
}
