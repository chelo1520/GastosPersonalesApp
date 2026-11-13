package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;
import java.util.List;

public class VistaMisGastos extends VistaWeb {

  public VistaMisGastos(Page page) {
    super(page);
  }

  // Filas de la lista por día (rediseño v2): "descripción importe", ej. "Compra semanal $ 46.200,00"
  public List<String> obtenerFilasDeLaTabla() {
    return page
      .locator("[data-grupo-dia] .app-mg-fila")
      .all()
      .stream()
      .map(fila ->
        (fila.locator(".app-mg-fila-descripcion").innerText() +
          " " +
          fila.locator(".app-mg-fila-importe").innerText()).replaceAll("\\s+", " ")
          .trim()
      )
      .toList();
  }

  public String obtenerTituloDelPrimerDia() {
    return page.locator("[data-grupo-dia] .app-mg-grupo-titulo").first().innerText().trim();
  }

  public String obtenerTotal() {
    return this.obtenerTextoDelElemento("#filtro-total").replaceAll("\\s+", " ").trim();
  }
}
