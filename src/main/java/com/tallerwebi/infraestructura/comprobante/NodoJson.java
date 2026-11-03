package com.tallerwebi.infraestructura.comprobante;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura tolerante de un nodo JSON escrito por el modelo: cada método devuelve el valor ya
 * validado, o null si falta o no sirve, en vez de lanzar una excepción.
 */
public final class NodoJson {

  private final JsonNode nodo;

  public NodoJson(JsonNode nodo) {
    this.nodo = nodo;
  }

  public NodoJson hijo(String campo) {
    return new NodoJson(nodo.path(campo));
  }

  // Los elementos de una lista; vacía si el campo no es una lista
  public List<NodoJson> elementos(String campo) {
    List<NodoJson> elementos = new ArrayList<>();
    for (JsonNode elemento : nodo.path(campo)) {
      elementos.add(new NodoJson(elemento));
    }
    return elementos;
  }

  // Un número finito y mayor a cero
  public Double importe(String campo) {
    JsonNode valor = nodo.path(campo);
    boolean valido = valor.isNumber() && Double.isFinite(valor.asDouble()) && valor.asDouble() > 0;
    return valido ? valor.asDouble() : null;
  }

  // Un texto con contenido y de hasta largoMaximo caracteres, sin los espacios de los bordes
  public String texto(String campo, int largoMaximo) {
    JsonNode valor = nodo.path(campo);
    if (!valor.isTextual()) {
      return null;
    }
    String texto = valor.asText().trim();
    return texto.isEmpty() || texto.length() > largoMaximo ? null : texto;
  }

  // Una fecha AAAA-MM-DD
  public LocalDate fecha(String campo) {
    JsonNode valor = nodo.path(campo);
    if (!valor.isTextual()) {
      return null;
    }
    try {
      return LocalDate.parse(valor.asText());
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
