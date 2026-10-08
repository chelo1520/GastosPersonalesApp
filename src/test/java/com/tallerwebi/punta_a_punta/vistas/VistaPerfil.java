package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;

public class VistaPerfil extends VistaWeb {

  public VistaPerfil(Page page) {
    super(page);
  }

  public String obtenerEmail() {
    return this.obtenerTextoDelElemento("#perfil-email");
  }
}
