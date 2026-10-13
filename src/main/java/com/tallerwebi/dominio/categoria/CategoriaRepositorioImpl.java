package com.tallerwebi.dominio.categoria;

import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class CategoriaRepositorioImpl implements CategoriaRepositorio {

  private final SessionFactory sessionFactory;

  @Autowired
  public CategoriaRepositorioImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public List<Categoria> obtenerTodas() {
    return sessionFactory
      .getCurrentSession()
      .createQuery("FROM Categoria ORDER BY nombre", Categoria.class)
      .getResultList();
  }

  @Override
  public Categoria buscarPorId(Long id) {
    return id == null ? null : sessionFactory.getCurrentSession().get(Categoria.class, id);
  }

  @Override
  @Transactional
  public void modificar(Categoria categoria) {
    sessionFactory.getCurrentSession().update(categoria);
  }
}
