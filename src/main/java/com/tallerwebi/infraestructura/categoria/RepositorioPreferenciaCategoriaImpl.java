package com.tallerwebi.infraestructura.categoria;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.PreferenciaCategoria;
import com.tallerwebi.dominio.categoria.RepositorioPreferenciaCategoria;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioPreferenciaCategoriaImpl implements RepositorioPreferenciaCategoria {

  private static final String USUARIO = "usuario";

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioPreferenciaCategoriaImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public List<PreferenciaCategoria> obtenerDelUsuario(Usuario usuario) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("FROM PreferenciaCategoria WHERE usuario = :usuario", PreferenciaCategoria.class)
      .setParameter(USUARIO, usuario)
      .getResultList();
  }

  @Override
  public PreferenciaCategoria buscar(Usuario usuario, Categoria categoria) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "FROM PreferenciaCategoria WHERE usuario = :usuario AND categoria = :categoria",
        PreferenciaCategoria.class
      )
      .setParameter(USUARIO, usuario)
      .setParameter("categoria", categoria)
      .uniqueResult();
  }

  @Override
  public void guardar(PreferenciaCategoria preferencia) {
    sessionFactory.getCurrentSession().persist(preferencia);
  }

  @Override
  public void modificar(PreferenciaCategoria preferencia) {
    sessionFactory.getCurrentSession().merge(preferencia);
  }
}
