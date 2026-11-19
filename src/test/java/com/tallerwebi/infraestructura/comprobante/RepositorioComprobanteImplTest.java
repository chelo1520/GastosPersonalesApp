package com.tallerwebi.infraestructura.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.comprobante.Comprobante;
import com.tallerwebi.dominio.comprobante.RepositorioComprobante;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.infraestructura.RepositorioUsuarioImpl;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
public class RepositorioComprobanteImplTest {

  private static final String HUELLA = "a".repeat(64);
  private static final LocalDateTime CARGA = LocalDateTime.of(2026, 11, 19, 10, 30);

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioComprobante repositorio;
  private Usuario ana;
  private Usuario luis;

  @BeforeEach
  public void init() {
    repositorio = new RepositorioComprobanteImpl(sessionFactory);
    ana = usuario("ana@unlam.edu.ar");
    luis = usuario("luis@unlam.edu.ar");
  }

  @Test
  public void debeEncontrarElComprobanteDelUsuarioPorSuHuella() {
    Comprobante comprobante = new Comprobante(ana, HUELLA, "image/png", CARGA);
    repositorio.guardar(comprobante);

    assertSame(comprobante, repositorio.buscar(ana, HUELLA));
    assertNull(repositorio.buscar(ana, "b".repeat(64)));
  }

  @Test
  public void porIdSoloEncuentraLosComprobantesDelUsuario() {
    Comprobante comprobante = new Comprobante(ana, HUELLA, "image/png", CARGA);
    repositorio.guardar(comprobante);

    assertSame(comprobante, repositorio.buscarPorId(comprobante.getId(), ana));
    assertNull(repositorio.buscarPorId(comprobante.getId(), luis));
  }

  @Test
  public void lasClavesDelUsuarioSonLasDeLosArchivosQueSeGuardaron() {
    Comprobante conArchivo = new Comprobante(ana, HUELLA, "image/png", CARGA);
    conArchivo.setClave("comprobantes/1/a.png");
    repositorio.guardar(conArchivo);
    repositorio.guardar(new Comprobante(ana, "b".repeat(64), "image/png", CARGA));
    Comprobante deLuis = new Comprobante(luis, HUELLA, "image/png", CARGA);
    deLuis.setClave("comprobantes/2/a.png");
    repositorio.guardar(deLuis);

    assertEquals(List.of("comprobantes/1/a.png"), repositorio.clavesDe(ana));
  }

  @Test
  public void elMismoArchivoDeOtroUsuarioNoCuenta() {
    repositorio.guardar(new Comprobante(ana, HUELLA, "image/png", CARGA));

    assertNull(repositorio.buscar(luis, HUELLA));
  }

  @Test
  public void debeContarLosGastosQueSiguenGuardadosConElComprobante() {
    Comprobante comprobante = new Comprobante(ana, HUELLA, "image/png", CARGA);
    repositorio.guardar(comprobante);
    assertEquals(0L, repositorio.contarGastos(comprobante));

    gastoCon(comprobante, 6300.0);
    gastoCon(comprobante, 3500.0);
    gastoCon(null, 1000.0);

    assertEquals(2L, repositorio.contarGastos(comprobante));
  }

  @Test
  public void alEliminarLaCuentaDebeBorrarSusComprobantes() {
    Comprobante comprobante = new Comprobante(ana, HUELLA, "image/png", CARGA);
    repositorio.guardar(comprobante);
    gastoCon(comprobante, 6300.0);
    // Como en la app: la baja llega en otro pedido, sin estos objetos cargados en la sesión
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();

    new RepositorioUsuarioImpl(sessionFactory).eliminar(ana);
    sessionFactory.getCurrentSession().flush();

    assertEquals(
      0L,
      sessionFactory
        .getCurrentSession()
        .createQuery("select count(c) from Comprobante c", Long.class)
        .getSingleResult()
    );
  }

  private void gastoCon(Comprobante comprobante, Double importe) {
    Gasto gasto = new Gasto(importe, LocalDate.of(2026, 11, 18), "Supermercado");
    gasto.setUsuario(ana);
    gasto.setComprobante(comprobante);
    sessionFactory.getCurrentSession().persist(gasto);
  }

  private Usuario usuario(String email) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword("123");
    sessionFactory.getCurrentSession().persist(usuario);
    return usuario;
  }
}
