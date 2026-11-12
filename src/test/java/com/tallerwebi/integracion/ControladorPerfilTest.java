package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
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
public class ControladorPerfilTest {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private SessionFactory sessionFactory;

  private MockMvc mockMvc;

  @BeforeEach
  public void init() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void debeRedirigirAlLoginCuandoNoExisteUnaSesion() throws Exception {
    mockMvc
      .perform(get("/perfil"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  @Test
  public void debeMostrarElEmailDelUsuarioAutenticado() throws Exception {
    Usuario usuario = new Usuario();
    usuario.setEmail("marce@ejemplo.com");
    usuario.setPassword("clave");
    // La tabla anual de presupuestos consulta por el usuario: tiene que existir en la base
    sessionFactory.getCurrentSession().persist(usuario);

    mockMvc
      .perform(get("/perfil").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(view().name("perfil"))
      .andExpect(content().string(containsString("marce@ejemplo.com")))
      .andExpect(content().string(not(containsString("clave"))));
  }

  @Test
  public void debeMostrarElEmailSinEstadoDeCuentaNiContrasenia() throws Exception {
    Usuario usuario = new Usuario();
    usuario.setEmail("marce@ejemplo.com");
    usuario.setPassword("clave");
    // La tabla anual de presupuestos consulta por el usuario: tiene que existir en la base
    sessionFactory.getCurrentSession().persist(usuario);

    mockMvc
      .perform(get("/perfil").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(view().name("perfil"))
      .andExpect(content().string(containsString("marce@ejemplo.com")))
      .andExpect(content().string(not(containsString("Estado de la cuenta"))))
      .andExpect(content().string(not(containsString("clave"))));
  }
}
