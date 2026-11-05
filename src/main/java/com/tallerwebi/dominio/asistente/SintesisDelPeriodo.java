package com.tallerwebi.dominio.asistente;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Respuesta estructurada del asistente, en dos niveles: un resumen corto que se ve siempre y el
 * detalle que el usuario despliega con "Contame más". El detalle puede faltar (null).
 */
public record SintesisDelPeriodo(
  @JsonPropertyDescription(
    "Una o dos oraciones, de 30 palabras como máximo, con el veredicto y lo más importante"
  )
  String resumen,
  @JsonPropertyDescription("Un párrafo que amplía el resumen sin repetirlo") String detalle
) {}
