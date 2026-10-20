package com.tallerwebi.infraestructura.recuperacion;

import com.tallerwebi.dominio.recuperacion.RepositorioTokenRecuperacion;
import com.tallerwebi.dominio.recuperacion.TokenRecuperacion;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/** Tokens de recuperación de contraseña en la base. */
@Repository
public class RepositorioTokenRecuperacionImpl implements RepositorioTokenRecuperacion {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioTokenRecuperacionImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardar(TokenRecuperacion token) {
    sessionFactory.getCurrentSession().persist(token);
  }

  @Override
  public TokenRecuperacion buscarPorHash(String hash) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from TokenRecuperacion where hash = :hash", TokenRecuperacion.class)
      .setParameter("hash", hash)
      .uniqueResult();
  }

  @Override
  public void modificar(TokenRecuperacion token) {
    sessionFactory.getCurrentSession().merge(token);
  }
}
