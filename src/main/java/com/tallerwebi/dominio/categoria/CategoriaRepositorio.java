package com.tallerwebi.dominio.categoria;

import java.util.List;

public interface CategoriaRepositorio {
  List<Categoria> obtenerTodas();

  Categoria buscarPorId(Long id);
}
