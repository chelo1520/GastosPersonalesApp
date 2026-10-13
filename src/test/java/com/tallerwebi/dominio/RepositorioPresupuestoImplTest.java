package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.RepositorioPresupuesto;
import com.tallerwebi.dominio.presupuesto.RepositorioPresupuestoImpl;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import java.time.YearMonth;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
@Transactional
@Rollback
public class RepositorioPresupuestoImplTest {

  private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);
  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioPresupuesto repositorioPresupuesto;
  private Usuario diego;
  private Usuario otroUsuario;
  private Categoria alimentacion;
  private Categoria transporte;

  @BeforeEach
  public void init() {
    repositorioPresupuesto = new RepositorioPresupuestoImpl(sessionFactory);

    diego = usuario("diego@unlam.edu.ar");
    otroUsuario = usuario("otro@unlam.edu.ar");
    alimentacion = categoria("Alimentación");
    transporte = categoria("Transporte");
  }

  @Test
  void debeEncontrarElPresupuestoPorUsuarioCategoriaYMesCuandoSeGuarda() {
    repositorioPresupuesto.guardar(new Presupuesto(diego, alimentacion, OCTUBRE, 130000.0));

    Presupuesto encontrado = repositorioPresupuesto.buscar(diego, alimentacion, OCTUBRE);

    assertThat(encontrado, notNullValue());
    assertThat(encontrado.getMonto(), equalTo(130000.0));
    assertThat(encontrado.getMes(), equalTo(OCTUBRE));
  }

  @Test
  void debeDevolverNullCuandoLaCategoriaNoTienePresupuestoEnEseMes() {
    repositorioPresupuesto.guardar(new Presupuesto(diego, alimentacion, SEPTIEMBRE, 130000.0));

    assertThat(repositorioPresupuesto.buscar(diego, alimentacion, OCTUBRE), nullValue());
    assertThat(repositorioPresupuesto.buscar(diego, transporte, SEPTIEMBRE), nullValue());
  }

  @Test
  void debeDevolverSoloLosPresupuestosDelUsuarioYDelMesCuandoHayDeOtrosUsuariosYMeses() {
    repositorioPresupuesto.guardar(new Presupuesto(diego, alimentacion, OCTUBRE, 130000.0));
    repositorioPresupuesto.guardar(new Presupuesto(diego, transporte, OCTUBRE, 60000.0));
    repositorioPresupuesto.guardar(new Presupuesto(diego, alimentacion, SEPTIEMBRE, 99000.0));
    repositorioPresupuesto.guardar(new Presupuesto(otroUsuario, alimentacion, OCTUBRE, 1.0));

    var delMes = repositorioPresupuesto.obtenerDelMes(diego, OCTUBRE);

    assertThat(delMes, hasSize(2));
    assertThat(
      delMes.stream().map(p -> p.getCategoria().getNombre()).toList(),
      contains("Alimentación", "Transporte")
    );
  }

  @Test
  void debeGuardarElMontoNuevoCuandoSeModificaUnPresupuesto() {
    Presupuesto presupuesto = new Presupuesto(diego, alimentacion, OCTUBRE, 130000.0);
    repositorioPresupuesto.guardar(presupuesto);

    presupuesto.setMonto(150000.0);
    repositorioPresupuesto.modificar(presupuesto);
    sessionFactory.getCurrentSession().flush();

    assertThat(
      repositorioPresupuesto.buscar(diego, alimentacion, OCTUBRE).getMonto(),
      equalTo(150000.0)
    );
  }

  private Usuario usuario(String email) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword("123");
    sessionFactory.getCurrentSession().persist(usuario);
    return usuario;
  }

  private Categoria categoria(String nombre) {
    Categoria categoria = new Categoria();
    categoria.setNombre(nombre);
    sessionFactory.getCurrentSession().persist(categoria);
    return categoria;
  }
}
