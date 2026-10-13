package com.tallerwebi.dominio.asistente;

import com.tallerwebi.dominio.consejo.Consejo;
import java.util.List;

/**
 * ServicioAsistente: escribe la síntesis del período a partir de los consejos ya calculados por
 * las reglas. No calcula nada: recibe hechos y devuelve un párrafo que los conecta.
 * Si no está disponible (sin clave, sin red, tope del día) devuelve null y se muestra el veredicto
 * de las reglas.
 */
public interface ServicioAsistente {
  String sintetizar(List<Consejo> consejos, GastoReal contexto);

  boolean disponible();
}
