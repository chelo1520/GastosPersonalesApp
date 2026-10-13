package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.tallerwebi.dominio.consejo.Consejo;
import com.tallerwebi.dominio.consejo.EstadoConsejo;
import com.tallerwebi.dominio.consejo.RepositorioConsejo;
import com.tallerwebi.dominio.consejo.RepositorioConsejoImpl;
import com.tallerwebi.dominio.consejo.Severidad;
import com.tallerwebi.dominio.consejo.TipoConsejo;
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
public class RepositorioConsejoImplTest {

  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioConsejo repositorio;
  private Usuario diego;
  private Usuario otro;

  @BeforeEach
  public void init() {
    repositorio = new RepositorioConsejoImpl(sessionFactory);
    diego = usuario("diego@unlam.edu.ar");
    otro = usuario("otro@unlam.edu.ar");
  }

  @Test
  void debeDevolverSoloLosConsejosDelUsuarioYDelMesCuandoSeGuardanVarios() {
    repositorio.guardar(consejo(diego, OCTUBRE, "Alimentación"));
    repositorio.guardar(consejo(diego, OCTUBRE, "Transporte"));
    repositorio.guardar(consejo(diego, OCTUBRE.minusMonths(1), "Alimentación"));
    repositorio.guardar(consejo(otro, OCTUBRE, "Alimentación"));

    assertThat(repositorio.obtenerDelMes(diego, OCTUBRE), hasSize(2));
  }

  @Test
  void debeConservarElEstadoCuandoSeModificaUnConsejo() {
    Consejo consejo = consejo(diego, OCTUBRE, "Alimentación");
    repositorio.guardar(consejo);

    consejo.setEstado(EstadoConsejo.APLICADO);
    repositorio.modificar(consejo);
    sessionFactory.getCurrentSession().flush();

    assertThat(
      repositorio.buscarPorId(consejo.getId()).getEstado(),
      equalTo(EstadoConsejo.APLICADO)
    );
  }

  private Consejo consejo(Usuario usuario, YearMonth mes, String categoria) {
    return new Consejo(
      usuario,
      mes,
      TipoConsejo.PRESUPUESTO_DESACTUALIZADO,
      Severidad.ATENCION,
      categoria,
      "texto",
      1000.0
    );
  }

  private Usuario usuario(String email) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword("123");
    sessionFactory.getCurrentSession().persist(usuario);
    return usuario;
  }
}
