package com.tallerwebi.dominio.consejo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * El párrafo que escribió la IA sobre el período (solo en el veredicto) y con qué hechos se escribió:
 * si cambian, la síntesis ya no vale.
 */
@Embeddable
public class Sintesis {

  @Column(name = "textoRedactado", length = 1000)
  private String texto;

  @Column(name = "firmaHechos")
  private String firma;

  public Sintesis() {}

  public Sintesis(String texto, String firma) {
    this.texto = texto;
    this.firma = firma;
  }

  public String getTexto() {
    return texto;
  }

  public String getFirma() {
    return firma;
  }
}
