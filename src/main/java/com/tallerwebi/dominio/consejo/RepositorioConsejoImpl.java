package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import java.time.YearMonth;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * RepositorioConsejoImpl
 */
@Repository
public class RepositorioConsejoImpl implements RepositorioConsejo {

  private SessionFactory sessionFactory;

  @Autowired
  public RepositorioConsejoImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardar(Consejo consejo) {
    sessionFactory.getCurrentSession().save(consejo);
  }

  @Override
  public void modificar(Consejo consejo) {
    sessionFactory.getCurrentSession().update(consejo);
  }

  @Override
  public Consejo buscarPorId(Long id) {
    return id == null ? null : sessionFactory.getCurrentSession().get(Consejo.class, id);
  }

  @Override
  public List<Consejo> obtenerDelMes(Usuario usuario, YearMonth mes) {
    String hql = "FROM Consejo WHERE usuario = :usuario AND mes = :mes ORDER BY id";
    Query<Consejo> query = sessionFactory.getCurrentSession().createQuery(hql, Consejo.class);
    query.setParameter("usuario", usuario);
    query.setParameter("mes", mes.toString());
    return query.getResultList();
  }
}
