package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.asistente.GastoReal;
import java.util.List;

/**
 * ServicioConsejos: las reglas del asistente, calculadas en Java sobre el resumen,
 * los presupuestos y los indicadores
 */
public interface ServicioConsejos {
  // Genera (o reutiliza) los consejos del mes en curso sobre el análisis del período por defecto
  List<Consejo> obtenerPendientes(Usuario usuario);

  // Igual, pero sobre un análisis ya calculado: así el veredicto habla del período que se está viendo.
  // No llama a la IA: si la síntesis guardada ya no vale, el veredicto vuelve al texto de las reglas
  List<Consejo> obtenerPendientes(Usuario usuario, GastoReal gastoReal);

  // La síntesis del período escrita por la IA (o null si no está disponible). Se pide aparte porque
  // tarda: la vista se arma con las reglas y la pide en segundo plano. Se guarda en el veredicto
  String sintetizar(Usuario usuario, GastoReal gastoReal);

  // Hay veredicto, la IA está disponible y todavía no se escribió la síntesis de estos hechos
  boolean faltaLaSintesis(List<Consejo> consejos);

  void descartar(Long id, Usuario usuario);

  // Aplica la acción del consejo (hoy: actualizar el presupuesto al monto sugerido)
  void aplicar(Long id, Usuario usuario);
}
