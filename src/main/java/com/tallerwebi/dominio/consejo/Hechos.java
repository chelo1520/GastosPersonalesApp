package com.tallerwebi.dominio.consejo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Lo que calculó la regla: un título corto para listas y el texto completo con los números. */
@Embeddable
public class Hechos {

  // Para la tira, el acordeón y los puntos numerados del gráfico (ej. "Servicios bajó 22,5 % real")
  @Column(name = "titulo")
  private String titulo;

  // Texto de las reglas: los hechos, siempre disponibles
  @Column(name = "texto")
  private String texto;

  public Hechos() {}

  public Hechos(String titulo, String texto) {
    this.titulo = titulo;
    this.texto = texto;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getTexto() {
    return texto;
  }
}
