package com.tallerwebi.dominio.comprobante;

/** Dónde se guardan los archivos de los comprobantes (Cloudflare R2). */
public interface AlmacenComprobantes {
  // false si no está configurado: los gastos se guardan igual, sin el archivo
  boolean disponible();

  // Lanza AlmacenNoDisponible si no se pudo guardar
  void guardar(String clave, byte[] contenido, String tipo);
}
