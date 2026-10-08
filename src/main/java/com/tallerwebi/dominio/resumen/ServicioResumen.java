package com.tallerwebi.dominio.resumen;

import com.tallerwebi.dominio.Usuario;
import java.time.YearMonth;

/**
 * ServicioResumen
 */
// Es la interfaz de un servicio, no una funcional: tiene un solo método por ahora
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ServicioResumen {
  ResumenMensual obtenerResumen(Usuario usuario, YearMonth mes);
}
