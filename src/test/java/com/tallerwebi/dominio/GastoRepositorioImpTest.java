package com.tallerwebi.dominio;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import com.tallerwebi.config.HibernateConfig;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.tallerwebi.dominio.gasto.Gasto;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateConfig.class)
@Transactional
public class GastoRepositorioImpTest {

    @Autowired
    private SessionFactory sessionFactory;
    private RepositorioGasto repositorioGasto;

    @BeforeEach
    public void init() {
        this.repositorioGasto = new RepositorioGastoImpl(this.sessionFactory);
    }


    @Test 
    void deberiaGuardarUnGasto(){
        Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado");

        repositorioGasto.guardar(gasto);

        String hql = "FROM Gasto WHERE descripcion = :descripcion";
        Query query = this.sessionFactory.getCurrentSession().createQuery(hql, Gasto.class);
        query.setParameter("descripcion", "Supermercado");
        Gasto gastoBuscado = (Gasto) query.getSingleResult();

        assertThat(gastoBuscado.getDescripcion(), equalTo(gasto.getDescripcion()));
    }


    @Test 
    void deberiaGuardarImporteFechaYDescripcion(){
                Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado");

                repositorioGasto.guardar(gasto);

                Gasto gastoGuardado = this.sessionFactory.getCurrentSession().get(Gasto.class, gasto.getId());

                assertEquals("Supermercado", gastoGuardado.getDescripcion());
                assertEquals(1000.00, gastoGuardado.getImporte());
                assertEquals(LocalDate.of(2026, 9, 13), gastoGuardado.getFecha());
    }
 
}
