package com.tallerwebi.infraestructura.categoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.PreferenciaCategoria;
import com.tallerwebi.dominio.categoria.RepositorioPreferenciaCategoria;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
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
@ContextConfiguration(classes = { HibernateInfraestructuraTestConfig.class })
public class RepositorioPreferenciaCategoriaTest {

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioPreferenciaCategoria repositorio;
  private Usuario medio;
  private Usuario alto;
  private Categoria delivery;

  @BeforeEach
  public void init() {
    repositorio = new RepositorioPreferenciaCategoriaImpl(sessionFactory);
  }

  @Test
  @Transactional
  @Rollback
  public void debeEncontrarLaPreferenciaGuardadaPorUsuarioYCategoria() {
    dadosDosUsuariosYUnaCategoria();

    repositorio.guardar(new PreferenciaCategoria(medio, delivery, true));

    PreferenciaCategoria guardada = repositorio.buscar(medio, delivery);
    assertTrue(guardada.getEsencial());
    assertEquals(1, repositorio.obtenerDelUsuario(medio).size());
  }

  @Test
  @Transactional
  @Rollback
  public void laMarcaDeUnUsuarioNoAfectaALosDemas() {
    dadosDosUsuariosYUnaCategoria();

    repositorio.guardar(new PreferenciaCategoria(medio, delivery, true));

    assertNull(repositorio.buscar(alto, delivery));
    assertTrue(repositorio.obtenerDelUsuario(alto).isEmpty());
  }

  @Test
  @Transactional
  @Rollback
  public void debeActualizarLaPreferenciaAlModificarla() {
    dadosDosUsuariosYUnaCategoria();
    PreferenciaCategoria preferencia = new PreferenciaCategoria(medio, delivery, true);
    repositorio.guardar(preferencia);

    preferencia.setEsencial(false);
    repositorio.modificar(preferencia);

    assertFalse(repositorio.buscar(medio, delivery).getEsencial());
    assertEquals(1, repositorio.obtenerDelUsuario(medio).size());
  }

  private void dadosDosUsuariosYUnaCategoria() {
    medio = usuario("medio@gastitos.demo");
    alto = usuario("alto@gastitos.demo");
    delivery = new Categoria();
    delivery.setNombre("Delivery");
    delivery.setEsencial(false);
    sessionFactory.getCurrentSession().persist(delivery);
  }

  private Usuario usuario(String email) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword("test");
    sessionFactory.getCurrentSession().persist(usuario);
    return usuario;
  }
}
