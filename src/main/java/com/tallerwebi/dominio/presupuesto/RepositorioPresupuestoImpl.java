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

  private static final String USUARIO = "usuario";

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

    query.setParameter(USUARIO, usuario);
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

    query.setParameter(USUARIO, usuario);
    query.setParameter("mes", mes.toString());

    return query.getResultList();
  }

  @Override
  public List<Presupuesto> obtenerDelUsuario(Usuario usuario) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("FROM Presupuesto WHERE usuario = :usuario", Presupuesto.class)
      .setParameter(USUARIO, usuario)
      .getResultList();
  }

  @Override
  public void guardarTotal(PresupuestoTotal total) {
    sessionFactory.getCurrentSession().saveOrUpdate(total);
  }

  @Override
  public PresupuestoTotal buscarTotal(Usuario usuario, YearMonth mes) {
    // Un usuario sin guardar no puede tener totales
    if (usuario.getId() == null) {
      return null;
    }
    Query<PresupuestoTotal> query = sessionFactory
      .getCurrentSession()
      .createQuery(
        "FROM PresupuestoTotal WHERE usuario = :usuario AND mes = :mes",
        PresupuestoTotal.class
      );

    query.setParameter(USUARIO, usuario);
    query.setParameter("mes", mes.toString());

    return query.uniqueResult();
  }
}
