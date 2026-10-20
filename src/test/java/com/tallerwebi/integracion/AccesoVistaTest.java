package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.util.List;
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
public class AccesoVistaTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private SessionFactory sessionFactory;

  private MockMvc mockMvc;

  @BeforeEach
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  public void debeMostrarElLoginConElLinkACrearUnaCuentaYSinSelectorDePestanas() throws Exception {
    mockMvc
      .perform(get("/login"))
      .andExpect(status().isOk())
      .andExpect(content().string(not(containsString("tab-login"))))
      .andExpect(content().string(containsString("href=\"/nuevo-usuario\">Crear una cuenta")))
      .andExpect(content().string(containsString("id=\"btn-login\"")));
  }

  @Test
  public void debeMostrarElRegistroConElLinkAIniciarSesionYSinSelectorDePestanas()
    throws Exception {
    mockMvc
      .perform(get("/nuevo-usuario"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Creá tu cuenta")))
      .andExpect(content().string(not(containsString("tab-registro"))))
      .andExpect(content().string(containsString("href=\"/login\">Iniciá sesión")))
      .andExpect(content().string(containsString("id=\"confirmacion\"")))
      .andExpect(content().string(containsString("id=\"password-requisitos\"")))
      .andExpect(content().string(containsString("data-regla=\"mayuscula\"")))
      .andExpect(content().string(containsString("data-regla=\"especial\"")))
      .andExpect(content().string(containsString("id=\"btn-registrarme\"")));
  }

  @Test
  public void debeCrearLaCuentaYMostrarElAvisoEnElLogin() throws Exception {
    mockMvc
      .perform(
        post("/registrarme").param("email", "nueva@gastitos.com").param("password", "Secreta1!")
      )
      .andExpect(redirectedUrl("/login?registrado"));

    assertEquals(1, usuariosConEmail("nueva@gastitos.com").size());

    mockMvc
      .perform(get("/login").param("registrado", ""))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Tu cuenta ya está creada")));
  }

  @Test
  public void noDebeCrearOtraCuentaConUnEmailYaRegistradoAunqueCambieLaContrasena()
    throws Exception {
    Usuario existente = new Usuario();
    existente.setEmail("lour@gastitos.com");
    existente.setPassword("primera1");
    sessionFactory.getCurrentSession().persist(existente);

    mockMvc
      .perform(
        post("/registrarme").param("email", "lour@gastitos.com").param("password", "Segunda2!")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("nuevo-usuario"))
      .andExpect(content().string(containsString("El usuario ya existe")))
      // Se vuelve con el email cargado para corregirlo
      .andExpect(content().string(containsString("value=\"lour@gastitos.com\"")));

    assertEquals(1, usuariosConEmail("lour@gastitos.com").size());
  }

  private List<Usuario> usuariosConEmail(String email) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Usuario where email = :email", Usuario.class)
      .setParameter("email", email)
      .list();
  }
}
