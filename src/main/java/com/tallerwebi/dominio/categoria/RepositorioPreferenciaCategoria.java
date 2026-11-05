package com.tallerwebi.dominio.categoria;

import com.tallerwebi.dominio.Usuario;
import java.util.List;

public interface RepositorioPreferenciaCategoria {
  List<PreferenciaCategoria> obtenerDelUsuario(Usuario usuario);

  PreferenciaCategoria buscar(Usuario usuario, Categoria categoria);

  void guardar(PreferenciaCategoria preferencia);

  void modificar(PreferenciaCategoria preferencia);
}
