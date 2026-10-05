package com.tallerwebi.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicioImpl;
import com.tallerwebi.dominio.gasto.RepositorioGastoImpl;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import com.tallerwebi.presentacion.ControladorGasto;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

/**
 * Integra ControladorGasto, GastoServicioImpl y RepositorioGastoImpl contra la base en memoria.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
@Transactional
@Rollback
public class SimulacionIntegracionTest {

  @Autowired
  private SessionFactory sessionFactory;

  private ControladorGasto controladorGasto;
  private Usuario usuario;
  private MockHttpServletRequest request;
  private LocalDate mesAnterior;

  @BeforeEach
  public void init() {
    RepositorioGastoImpl repositorioGasto = new RepositorioGastoImpl(sessionFactory);
    controladorGasto = new ControladorGasto(new GastoServicioImpl(repositorioGasto));

    usuario = guardarUsuario("lourdes@gmail.com");
    request = requestConUsuario(usuario);
    mesAnterior = LocalDate.now().minusMonths(1);
  }

  @Test
  public void debeDevolverSoloElGastoDelMesAnteriorExcluyendoElActualYElDeHaceDosMeses() {
    guardarGasto(usuario, mesAnterior.withDayOfMonth(10), "Alquiler");
    guardarGasto(usuario, LocalDate.now(), "Gasto del mes actual");
    guardarGasto(usuario, mesAnterior.minusMonths(1), "Gasto de hace dos meses");

    List<Gasto> gastos = gastosDeLaSimulacion();

    assertEquals(1, gastos.size());
    assertEquals("Alquiler", gastos.get(0).getDescripcion());
  }

  @Test
  public void debeDevolverSoloElGastoPropioAunqueOtroUsuarioTengaGastosEnElMismoMes() {
    Usuario otroUsuario = guardarUsuario("otro@gmail.com");
    guardarGasto(usuario, mesAnterior.withDayOfMonth(10), "Gasto propio");
    guardarGasto(otroUsuario, mesAnterior.withDayOfMonth(10), "Gasto ajeno");

    List<Gasto> gastos = gastosDeLaSimulacion();

    assertEquals(1, gastos.size());
    assertEquals("Gasto propio", gastos.get(0).getDescripcion());
  }

  @Test
  public void debeDevolverLosGastosOrdenadosDeFinDeMesAPrincipioDeMes() {
    guardarGasto(usuario, mesAnterior.withDayOfMonth(1), "Primero del mes");
    guardarGasto(usuario, mesAnterior.withDayOfMonth(mesAnterior.lengthOfMonth()), "Fin de mes");
    guardarGasto(usuario, mesAnterior.withDayOfMonth(15), "Mitad de mes");

    List<Gasto> gastos = gastosDeLaSimulacion();

    assertEquals(
      List.of("Fin de mes", "Mitad de mes", "Primero del mes"),
      gastos.stream().map(Gasto::getDescripcion).toList()
    );
  }

  @Test
  public void debeIncluirEnLaSimulacionElGastoRegistradoConFechaDelMesAnterior() {
    Gasto gasto = new Gasto(25000.0, mesAnterior.withDayOfMonth(5), "Clases de guitarra");

    controladorGasto.registrarGasto(gasto, request);
    List<Gasto> gastos = gastosDeLaSimulacion();

    assertEquals(1, gastos.size());
    assertEquals("Clases de guitarra", gastos.get(0).getDescripcion());
    assertEquals(25000.0, gastos.get(0).getImporte());
    assertSame(usuario, gastos.get(0).getUsuario());
  }

  @SuppressWarnings("unchecked")
  private List<Gasto> gastosDeLaSimulacion() {
    ModelAndView resultado = controladorGasto.mostrarSimulacion(request);
    assertEquals("simulacion", resultado.getViewName());
    return (List<Gasto>) resultado.getModel().get("gastos");
  }

  private Usuario guardarUsuario(String email) {
    Usuario nuevoUsuario = new Usuario();
    nuevoUsuario.setEmail(email);
    nuevoUsuario.setPassword("1234");
    sessionFactory.getCurrentSession().save(nuevoUsuario);
    return nuevoUsuario;
  }

  private void guardarGasto(Usuario duenio, LocalDate fecha, String descripcion) {
    Gasto gasto = new Gasto(1000.0, fecha, descripcion);
    gasto.setUsuario(duenio);
    sessionFactory.getCurrentSession().save(gasto);
  }

  private MockHttpServletRequest requestConUsuario(Usuario usuarioLogueado) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("USUARIO", usuarioLogueado);
    MockHttpServletRequest nuevoRequest = new MockHttpServletRequest();
    nuevoRequest.setSession(session);
    return nuevoRequest;
  }
}
