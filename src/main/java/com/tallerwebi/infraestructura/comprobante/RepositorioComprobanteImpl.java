package com.tallerwebi.infraestructura.comprobante;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.comprobante.Comprobante;
import com.tallerwebi.dominio.comprobante.RepositorioComprobante;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/** Comprobantes cargados, en la base. */
@Repository
public class RepositorioComprobanteImpl implements RepositorioComprobante {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioComprobanteImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardar(Comprobante comprobante) {
    sessionFactory.getCurrentSession().persist(comprobante);
  }

  @Override
  public Comprobante buscar(Usuario usuario, String huella) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Comprobante where usuario = :usuario and huella = :huella",
        Comprobante.class
      )
      .setParameter("usuario", usuario)
      .setParameter("huella", huella)
      .uniqueResult();
  }

  @Override
  public Comprobante buscarPorId(Long id, Usuario usuario) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Comprobante where id = :id and usuario = :usuario", Comprobante.class)
      .setParameter("id", id)
      .setParameter("usuario", usuario)
      .uniqueResult();
  }

  @Override
  public List<String> clavesDe(Usuario usuario) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select clave from Comprobante where usuario = :usuario and clave is not null",
        String.class
      )
      .setParameter("usuario", usuario)
      .getResultList();
  }

  @Override
  public long contarGastos(Comprobante comprobante) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("select count(g) from Gasto g where g.comprobante = :comprobante", Long.class)
      .setParameter("comprobante", comprobante)
      .getSingleResult();
  }
}
