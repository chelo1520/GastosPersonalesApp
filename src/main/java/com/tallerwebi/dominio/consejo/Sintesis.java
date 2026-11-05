package com.tallerwebi.dominio.consejo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Lo que escribió la IA sobre el período (solo en el veredicto): un resumen corto, el detalle que se
 * despliega con "Contame más" y con qué hechos se escribió: si cambian, la síntesis ya no vale.
 */
@Embeddable
public class Sintesis {

  @Column(name = "resumenRedactado", length = 300)
  private String resumen;

  // El nombre de la columna viene de cuando la síntesis era un solo párrafo
  @Column(name = "textoRedactado", length = 1000)
  private String detalle;

  @Column(name = "firmaHechos")
  private String firma;

  public Sintesis() {}

  public Sintesis(String resumen, String detalle, String firma) {
    this.resumen = resumen;
    this.detalle = detalle;
    this.firma = firma;
  }

  public String getResumen() {
    return resumen;
  }

  public String getDetalle() {
    return detalle;
  }

  public String getFirma() {
    return firma;
  }
}
