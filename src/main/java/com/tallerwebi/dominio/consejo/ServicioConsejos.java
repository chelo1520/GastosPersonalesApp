package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import java.util.List;

/**
 * ServicioConsejos: las reglas del asistente, calculadas en Java sobre el resumen,
 * los presupuestos y los indicadores
 */
public interface ServicioConsejos {
  // Genera (o reutiliza) los consejos del mes en curso y devuelve los que siguen pendientes
  List<Consejo> obtenerPendientes(Usuario usuario);

  void descartar(Long id, Usuario usuario);

  // Aplica la acción del consejo (hoy: actualizar el presupuesto al monto sugerido)
  void aplicar(Long id, Usuario usuario);
}
