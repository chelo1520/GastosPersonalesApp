package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.consejo.Consejo;
import com.tallerwebi.dominio.consejo.TipoConsejo;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.recuperacion.TokenRecuperacion;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
@Transactional
@Rollback
public class PerfilCuentaVistaTest {

  private static final String EMAIL = "lour@gastitos.com";

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private SessionFactory sessionFactory;

  private MockMvc mockMvc;
  private Usuario usuario;

  @BeforeEach
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    usuario = new Usuario();
    usuario.setEmail(EMAIL);
    usuario.setPassword("Actual1!");
    usuario.setActivo(true);
    sessionFactory.getCurrentSession().persist(usuario);
  }

  @Test
  public void debeMostrarLosFormulariosDeCambiarContrasenaYEliminarCuenta() throws Exception {
    mockMvc
      .perform(get("/perfil").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Cambiar contraseña")))
      .andExpect(content().string(containsString("id=\"password-requisitos\"")))
      .andExpect(content().string(containsString("id=\"btn-cambiar-contrasena\"")))
      .andExpect(content().string(containsString("Eliminar cuenta")))
      .andExpect(content().string(containsString("data-confirmar-texto=")))
      .andExpect(content().string(containsString("data-confirmar-peligro")))
      .andExpect(content().string(containsString("id=\"modal-confirmar\"")))
      .andExpect(content().string(containsString("/js/perfil.js")))
      // Los dos desplegables arrancan cerrados
      .andExpect(content().string(not(containsString("open=\"open\""))));
  }

  @Test
  public void debeCambiarLaContrasenaYPoderIngresarConLaNueva() throws Exception {
    mockMvc
      .perform(
        post("/perfil/contrasena")
          .sessionAttr("USUARIO", usuario)
          .param("actual", "Actual1!")
          .param("password", "Nueva123!")
          .param("confirmacion", "Nueva123!")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/perfil?contrasenaCambiada"));

    mockMvc
      .perform(post("/validar-login").param("email", EMAIL).param("password", "Nueva123!"))
      .andExpect(redirectedUrl("/home"));
  }

  @Test
  public void noDebeCambiarlaConLaContrasenaActualIncorrecta() throws Exception {
    mockMvc
      .perform(
        post("/perfil/contrasena")
          .sessionAttr("USUARIO", usuario)
          .param("actual", "Mal1234!")
          .param("password", "Nueva123!")
          .param("confirmacion", "Nueva123!")
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("La contraseña actual no es correcta")))
      // Con un error, el desplegable se abre para que se vea
      .andExpect(content().string(containsString("open=\"open\"")));

    sessionFactory.getCurrentSession().clear();
    assertEquals("Actual1!", buscarUsuario().getPassword());
  }

  @Test
  public void debeEliminarLaCuentaConTodosSusDatosYVolverAlLogin() throws Exception {
    Session session = sessionFactory.getCurrentSession();
    Gasto gasto = new Gasto(1500.0, LocalDate.of(2026, 10, 5), "Café");
    gasto.setUsuario(usuario);
    session.persist(gasto);
    session.persist(new Presupuesto(usuario, null, YearMonth.of(2026, 10), 10000.0));
    session.persist(
      new Consejo(
        usuario,
        YearMonth.of(2026, 10),
        TipoConsejo.PRESUPUESTO_DESACTUALIZADO,
        null,
        "Revisá el presupuesto",
        null
      )
    );
    session.persist(new TokenRecuperacion("hash-de-prueba", usuario, LocalDateTime.now()));
    Usuario otro = new Usuario();
    otro.setEmail("otro@gastitos.com");
    session.persist(otro);
    Gasto ajeno = new Gasto(800.0, LocalDate.of(2026, 10, 6), "Ajeno");
    ajeno.setUsuario(otro);
    session.persist(ajeno);
    session.flush();
    // La baja llega en otro pedido: la sesión no tiene cargados los datos del usuario
    session.clear();

    mockMvc
      .perform(
        post("/perfil/eliminar").sessionAttr("USUARIO", usuario).param("password", "Actual1!")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login?cuentaEliminada"));

    // Lo que haría el commit de la transacción
    session.flush();
    session.clear();
    assertNull(buscarUsuario());
    for (String entidad : new String[] { "Presupuesto", "Consejo", "TokenRecuperacion" }) {
      assertEquals(0L, contar(entidad), entidad);
    }
    // Solo se borran los gastos del usuario eliminado
    assertEquals(1L, contar("Gasto"));
    assertNotNull(session.get(Usuario.class, otro.getId()));
  }

  @Test
  public void noDebeEliminarLaCuentaConLaContrasenaIncorrecta() throws Exception {
    mockMvc
      .perform(
        post("/perfil/eliminar").sessionAttr("USUARIO", usuario).param("password", "Mal1234!")
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("la cuenta no se eliminó")));

    sessionFactory.getCurrentSession().clear();
    assertNotNull(buscarUsuario());
  }

  @Test
  public void elLoginDebeAvisarQueLaCuentaSeElimino() throws Exception {
    mockMvc
      .perform(get("/login").param("cuentaEliminada", ""))
      .andExpect(content().string(containsString("Tu cuenta y todos tus datos se eliminaron")));
  }

  private Usuario buscarUsuario() {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Usuario where email = :email", Usuario.class)
      .setParameter("email", EMAIL)
      .uniqueResult();
  }

  private long contar(String entidad) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("select count(*) from " + entidad, Long.class)
      .uniqueResult();
  }
}
