package com.tallerwebi.dominio.comprobante;

import com.tallerwebi.dominio.Usuario;

/** Los comprobantes cargados por cada usuario. */
public interface RepositorioComprobante {
  void guardar(Comprobante comprobante);

  // El comprobante del usuario con esa huella, o null si nunca lo cargó
  Comprobante buscar(Usuario usuario, String huella);

  // Cuántos gastos siguen guardados con ese comprobante (se pueden haber borrado)
  long contarGastos(Comprobante comprobante);
}
