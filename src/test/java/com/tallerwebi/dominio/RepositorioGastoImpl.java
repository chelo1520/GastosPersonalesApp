package com.tallerwebi.dominio;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.tallerwebi.dominio.gasto.Gasto;

/**
 * RepositorioGastoImpl
 */
@Repository 
public class RepositorioGastoImpl implements  RepositorioGasto{

    private SessionFactory sessionFactory;

    @Autowired
    public RepositorioGastoImpl(SessionFactory sessionFactory) {
        this.sessionFactory =  sessionFactory;
    }

    @Override
    public void guardar(Gasto gasto) {
        sessionFactory.getCurrentSession().save(gasto);
    }

	@Override
	public List<Gasto> BuscarGastosPorUsuario(Usuario usuario) {
        String hql = "FROM Gasto WHERE usuario = :usuario";
        Query<Gasto> query = sessionFactory.getCurrentSession().createQuery(hql, Gasto.class);

        query.setParameter("usuario", usuario);
        return query.getResultList();

	}

}
