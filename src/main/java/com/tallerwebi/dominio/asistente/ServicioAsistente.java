package com.tallerwebi.dominio.asistente;

import com.tallerwebi.dominio.consejo.Consejo;
import java.util.List;

/**
 * ServicioAsistente: redacta y prioriza los consejos ya calculados por las reglas.
 * No calcula nada: recibe hechos y devuelve los mismos consejos con mejor texto y orden.
 * Si no está disponible (sin clave, sin red, tope del día) devuelve la lista tal cual.
 */
public interface ServicioAsistente {
  List<Consejo> redactar(List<Consejo> consejos, GastoReal contexto);

  boolean disponible();
}
