package com.tallerwebi.dominio.consejo;

/** Las dos familias de consejos: lo que el asistente cuenta y lo que propone hacer. */
public enum Familia {
  LECTURA("Lo que veo"),
  PROPUESTA("Lo que te propongo");

  private final String titulo;

  Familia(String titulo) {
    this.titulo = titulo;
  }

  public String getTitulo() {
    return titulo;
  }
}
