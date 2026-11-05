package com.tallerwebi.dominio.categoria;

import com.tallerwebi.dominio.Usuario;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ServicioCategoriaImpl
 */
@Service
@Transactional
public class ServicioCategoriaImpl implements ServicioCategoria {

  private final CategoriaRepositorio categoriaRepositorio;
  private final RepositorioPreferenciaCategoria repositorioPreferencia;

  @Autowired
  public ServicioCategoriaImpl(
    CategoriaRepositorio categoriaRepositorio,
    RepositorioPreferenciaCategoria repositorioPreferencia
  ) {
    this.categoriaRepositorio = categoriaRepositorio;
    this.repositorioPreferencia = repositorioPreferencia;
  }

  @Override
  public List<Categoria> obtenerTodas() {
    return categoriaRepositorio.obtenerTodas();
  }

  @Override
  public Categoria buscarPorId(Long id) {
    return categoriaRepositorio.buscarPorId(id);
  }

  @Override
  public MarcasHormiga marcasDe(Usuario usuario) {
    return MarcasHormiga.de(repositorioPreferencia.obtenerDelUsuario(usuario));
  }

  @Override
  public void marcarComoHormiga(Usuario usuario, Long categoriaId) {
    marcar(usuario, categoriaId, false);
  }

  @Override
  public void quitarDeHormiga(Usuario usuario, Long categoriaId) {
    marcar(usuario, categoriaId, true);
  }

  // La marca es de cada usuario: la categoría del maestro no se toca. Una fija es siempre esencial
  private void marcar(Usuario usuario, Long categoriaId, boolean esencial) {
    Categoria categoria = categoriaRepositorio.buscarPorId(categoriaId);
    if (categoria == null || categoria.isFija()) {
      return;
    }
    PreferenciaCategoria preferencia = repositorioPreferencia.buscar(usuario, categoria);
    if (preferencia == null) {
      repositorioPreferencia.guardar(new PreferenciaCategoria(usuario, categoria, esencial));
      return;
    }
    preferencia.setEsencial(esencial);
    repositorioPreferencia.modificar(preferencia);
  }
}
