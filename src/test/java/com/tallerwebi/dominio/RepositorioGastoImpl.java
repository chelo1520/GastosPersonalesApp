package com.tallerwebi.dominio;

import org.hibernate.SessionFactory;
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

}
