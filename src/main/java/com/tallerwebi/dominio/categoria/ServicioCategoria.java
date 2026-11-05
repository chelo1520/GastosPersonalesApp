package com.tallerwebi.dominio.categoria;

import com.tallerwebi.dominio.Usuario;
import java.util.List;

public interface ServicioCategoria {
  List<Categoria> obtenerTodas();

  Categoria buscarPorId(Long id);

  /** Las marcas de hormiga del usuario: sus preferencias sobre el maestro de categorías. */
  MarcasHormiga marcasDe(Usuario usuario);

  /** La categoría pasa a prescindible para este usuario; una fija o inexistente se ignora. */
  void marcarComoHormiga(Usuario usuario, Long categoriaId);

  /** La categoría pasa a esencial para este usuario; una fija o inexistente se ignora. */
  void quitarDeHormiga(Usuario usuario, Long categoriaId);
}
