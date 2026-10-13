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

  // Igual, pero sobre un análisis ya calculado: así el veredicto habla del período que se está viendo
  List<Consejo> obtenerPendientes(Usuario usuario, GastoReal gastoReal);

  void descartar(Long id, Usuario usuario);

  // Aplica la acción del consejo (hoy: actualizar el presupuesto al monto sugerido)
  void aplicar(Long id, Usuario usuario);
}
