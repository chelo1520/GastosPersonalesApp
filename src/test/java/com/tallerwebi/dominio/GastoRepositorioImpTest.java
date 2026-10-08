package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.RepositorioGasto;
import com.tallerwebi.dominio.gasto.RepositorioGastoImpl;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
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
public class GastoRepositorioImpTest {

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioGasto repositorioGasto;

  @BeforeEach
  public void init() {
    this.repositorioGasto = new RepositorioGastoImpl(this.sessionFactory);
  }

  @Test
  void debeEncontrarElGastoPorSuDescripcionCuandoSeGuardaUnGasto() {
    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado");

    repositorioGasto.guardar(gasto);

    String hql = "FROM Gasto WHERE descripcion = :descripcion";
    Query query = this.sessionFactory.getCurrentSession().createQuery(hql, Gasto.class);
    query.setParameter("descripcion", "Supermercado");
    Gasto gastoBuscado = (Gasto) query.getSingleResult();

    assertThat(gastoBuscado.getDescripcion(), equalTo(gasto.getDescripcion()));
  }

  @Test
  void debePersistirImporteFechaYDescripcionCuandoSeGuardaUnGasto() {
    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado");

    repositorioGasto.guardar(gasto);

    Gasto gastoGuardado = this.sessionFactory.getCurrentSession().get(Gasto.class, gasto.getId());

    assertEquals("Supermercado", gastoGuardado.getDescripcion());
    assertEquals(1000.00, gastoGuardado.getImporte());
    assertEquals(LocalDate.of(2026, 9, 13), gastoGuardado.getFecha());
  }

  @Test
  void debeDevolverUnGastoCuandoElUsuarioTieneUnGastoGuardado() {
    Usuario usuario = new Usuario();
    usuario.setEmail("becerra@gmai.com");
    usuario.setPassword("1234");

    this.sessionFactory.getCurrentSession().save(usuario);

    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado");
    gasto.setUsuario(usuario);
    repositorioGasto.guardar(gasto);

    List<Gasto> gastos = repositorioGasto.BuscarGastosPorUsuario(usuario);

    assertEquals(1, gastos.size());
    assertEquals("Supermercado", gastos.get(0).getDescripcion());
  }

  @Test
  void deberiaDevolverSoloLosDosGastosDeSeptiembreDelUsuarioDelMasNuevoAlMasViejo() {
    Usuario usuario = new Usuario();
    usuario.setEmail("becerra@gmai.com");
    usuario.setPassword("1234");
    Usuario otroUsuario = new Usuario();
    otroUsuario.setEmail("otro@gmail.com");
    otroUsuario.setPassword("1234");
    this.sessionFactory.getCurrentSession().save(usuario);
    this.sessionFactory.getCurrentSession().save(otroUsuario);

    guardarGasto(usuario, LocalDate.of(2026, 9, 1), "Alquiler");
    guardarGasto(usuario, LocalDate.of(2026, 9, 30), "Supermercado");
    guardarGasto(usuario, LocalDate.of(2026, 8, 31), "Gasto de agosto");
    guardarGasto(usuario, LocalDate.of(2026, 10, 1), "Gasto de octubre");
    guardarGasto(otroUsuario, LocalDate.of(2026, 9, 15), "Gasto de otro usuario");

    List<Gasto> gastos = repositorioGasto.obtenerGastosDelMes(usuario, YearMonth.of(2026, 9));

    assertEquals(2, gastos.size());
    assertEquals("Supermercado", gastos.get(0).getDescripcion());
    assertEquals("Alquiler", gastos.get(1).getDescripcion());
  }

  @Test
  void deberiaDevolverUnaListaVaciaSiElUsuarioNoTieneGastosEnElMes() {
    Usuario usuario = new Usuario();
    usuario.setEmail("becerra@gmai.com");
    usuario.setPassword("1234");
    this.sessionFactory.getCurrentSession().save(usuario);

    guardarGasto(usuario, LocalDate.of(2026, 10, 5), "Gasto de octubre");

    List<Gasto> gastos = repositorioGasto.obtenerGastosDelMes(usuario, YearMonth.of(2026, 9));

    assertTrue(gastos.isEmpty());
  }

  @Test
  void deberiaDevolverElGastoDel29DeFebreroYExcluirElDeMarzoEnUnAnioBisiesto() {
    Usuario usuario = new Usuario();
    usuario.setEmail("becerra@gmai.com");
    usuario.setPassword("1234");
    this.sessionFactory.getCurrentSession().save(usuario);

    guardarGasto(usuario, LocalDate.of(2028, 2, 29), "Gasto del 29 de febrero");
    guardarGasto(usuario, LocalDate.of(2028, 3, 1), "Gasto de marzo");

    List<Gasto> gastos = repositorioGasto.obtenerGastosDelMes(usuario, YearMonth.of(2028, 2));

    assertEquals(1, gastos.size());
    assertEquals("Gasto del 29 de febrero", gastos.get(0).getDescripcion());
  }

  @Test
  void debePersistirLosNuevosDatosCuandoSeModificaUnGastoGuardado() {
    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado");
    repositorioGasto.guardar(gasto);

    Gasto gastoAModificar = repositorioGasto.buscarPorId(gasto.getId());
    gastoAModificar.setImporte(2500.00);
    gastoAModificar.setFecha(LocalDate.of(2026, 9, 20));
    gastoAModificar.setDescripcion("Farmacia");
    repositorioGasto.modificar(gastoAModificar);

    this.sessionFactory.getCurrentSession().flush();
    this.sessionFactory.getCurrentSession().clear();
    Gasto gastoGuardado = repositorioGasto.buscarPorId(gasto.getId());

    assertEquals(2500.00, gastoGuardado.getImporte());
    assertEquals(LocalDate.of(2026, 9, 20), gastoGuardado.getFecha());
    assertEquals("Farmacia", gastoGuardado.getDescripcion());
  }

  @Test
  void debeDevolverNullCuandoSeBuscaUnGastoQueNoExiste() {
    assertNull(repositorioGasto.buscarPorId(999L));
  }

  private void guardarGasto(Usuario usuario, LocalDate fecha, String descripcion) {
    Gasto gasto = new Gasto(1000.00, fecha, descripcion);
    gasto.setUsuario(usuario);
    repositorioGasto.guardar(gasto);
  }
}
