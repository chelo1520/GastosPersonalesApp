package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
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
public class MostrarGastosVistaTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private SessionFactory sessionFactory;

  @Test
  public void debeRenderizarLosGastosDelUsuarioEnLaVista() throws Exception {
    Usuario usuario = new Usuario();
    Gasto gasto = new Gasto(1800.0, LocalDate.of(2026, 9, 30), "Compra de prueba");
    gasto.setUsuario(usuario);

    sessionFactory.getCurrentSession().persist(usuario);
    sessionFactory.getCurrentSession().persist(gasto);

    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc
      .perform(get("/mostrar-gastos").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(view().name("mostrar-gastos"))
      .andExpect(content().string(containsString("Compra de prueba")))
      .andExpect(content().string(containsString("30/09/2026")))
      .andExpect(content().string(containsString("1.800,00")));
  }

  @Test
  public void debeRenderizarElFormularioDeModificarPrecargadoDesdeElLinkEditarDelListado()
    throws Exception {
    Usuario usuario = new Usuario();
    Gasto gasto = new Gasto(2300.5, LocalDate.of(2026, 9, 15), "Gasto a editar");
    gasto.setUsuario(usuario);

    sessionFactory.getCurrentSession().persist(usuario);
    sessionFactory.getCurrentSession().persist(gasto);

    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    String urlEditar = "/modificar-gasto/" + gasto.getId();

    mockMvc
      .perform(get("/mostrar-gastos").sessionAttr("USUARIO", usuario))
      .andExpect(content().string(containsString("href=\"" + urlEditar + "\"")));

    mockMvc
      .perform(get(urlEditar).sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(view().name("registrar-gasto"))
      .andExpect(content().string(containsString("Modificar gasto")))
      .andExpect(content().string(containsString("action=\"" + urlEditar + "\"")))
      .andExpect(content().string(containsString("value=\"Gasto a editar\"")))
      .andExpect(content().string(containsString("value=\"2026-09-15\"")))
      .andExpect(content().string(containsString("href=\"/mostrar-gastos\"")));
  }
}
