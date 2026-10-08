package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * RepositorioGastoImpl
 */
@Repository
public class RepositorioGastoImpl implements RepositorioGasto {

  private SessionFactory sessionFactory;

  @Autowired
  public RepositorioGastoImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardar(Gasto gasto) {
    sessionFactory.getCurrentSession().save(gasto);
  }

  @Override
  public void modificar(Gasto gasto) {
    sessionFactory.getCurrentSession().update(gasto);
  }

  @Override
  public Gasto buscarPorId(Long id) {
    return sessionFactory.getCurrentSession().get(Gasto.class, id);
  }

  @Override
  public List<Gasto> BuscarGastosPorUsuario(Usuario usuario) {
    String hql = "FROM Gasto WHERE usuario = :usuario";
    Query<Gasto> query = sessionFactory.getCurrentSession().createQuery(hql, Gasto.class);

    query.setParameter("usuario", usuario);
    return query.getResultList();
  }

  @Override
  public List<Gasto> obtenerGastosEntreFechas(Usuario usuario, LocalDate desde, LocalDate hasta) {
    String hql = "FROM Gasto WHERE usuario = :usuario AND fecha BETWEEN :desde AND :hasta";
    Query<Gasto> query = sessionFactory.getCurrentSession().createQuery(hql, Gasto.class);

    query.setParameter("usuario", usuario);
    query.setParameter("desde", desde);
    query.setParameter("hasta", hasta);

    return query.getResultList();
  }

  @Override
  public List<Gasto> obtenerGastosDelMes(Usuario usuario, YearMonth mes) {
    String hql =
      "FROM Gasto WHERE usuario = :usuario AND fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC";
    Query<Gasto> query = sessionFactory.getCurrentSession().createQuery(hql, Gasto.class);

    query.setParameter("usuario", usuario);
    query.setParameter("desde", mes.atDay(1));
    query.setParameter("hasta", mes.atEndOfMonth());

    return query.getResultList();
  }
}
