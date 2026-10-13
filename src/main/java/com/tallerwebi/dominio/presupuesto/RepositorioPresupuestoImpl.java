package com.tallerwebi.dominio.presupuesto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import java.time.YearMonth;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * RepositorioPresupuestoImpl
 */
@Repository
public class RepositorioPresupuestoImpl implements RepositorioPresupuesto {

  private SessionFactory sessionFactory;

  @Autowired
  public RepositorioPresupuestoImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardar(Presupuesto presupuesto) {
    sessionFactory.getCurrentSession().save(presupuesto);
  }

  @Override
  public void modificar(Presupuesto presupuesto) {
    sessionFactory.getCurrentSession().update(presupuesto);
  }

  @Override
  public Presupuesto buscar(Usuario usuario, Categoria categoria, YearMonth mes) {
    String hql =
      "FROM Presupuesto WHERE usuario = :usuario AND categoria = :categoria AND mes = :mes";
    Query<Presupuesto> query = sessionFactory
      .getCurrentSession()
      .createQuery(hql, Presupuesto.class);

    query.setParameter("usuario", usuario);
    query.setParameter("categoria", categoria);
    query.setParameter("mes", mes.toString());

    return query.uniqueResult();
  }

  @Override
  public List<Presupuesto> obtenerDelMes(Usuario usuario, YearMonth mes) {
    String hql =
      "FROM Presupuesto WHERE usuario = :usuario AND mes = :mes ORDER BY categoria.nombre";
    Query<Presupuesto> query = sessionFactory
      .getCurrentSession()
      .createQuery(hql, Presupuesto.class);

    query.setParameter("usuario", usuario);
    query.setParameter("mes", mes.toString());

    return query.getResultList();
  }
}
