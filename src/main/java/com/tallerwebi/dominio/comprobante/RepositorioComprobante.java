package com.tallerwebi.dominio.comprobante;

import com.tallerwebi.dominio.Usuario;
import java.util.List;

/** Los comprobantes cargados por cada usuario. */
public interface RepositorioComprobante {
  void guardar(Comprobante comprobante);

  // El comprobante del usuario con esa huella, o null si nunca lo cargó
  Comprobante buscar(Usuario usuario, String huella);

  // El comprobante con ese id si es del usuario, o null
  Comprobante buscarPorId(Long id, Usuario usuario);

  // Dónde quedaron en el almacén los archivos del usuario (los que se pudieron guardar)
  List<String> clavesDe(Usuario usuario);

  // Cuántos gastos siguen guardados con ese comprobante (se pueden haber borrado)
  long contarGastos(Comprobante comprobante);
}
