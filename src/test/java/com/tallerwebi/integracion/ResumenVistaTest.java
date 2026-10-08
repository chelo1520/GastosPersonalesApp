package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
public class ResumenVistaTest {

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
  public void debeRenderizarElTotalYLaCategoriaDeLosGastosDelMesPedido() throws Exception {
    Usuario usuario = new Usuario();
    Gasto alquiler = new Gasto(250000.0, LocalDate.of(2026, 9, 1), "Alquiler");
    Gasto farmacia = new Gasto(9800.0, LocalDate.of(2026, 9, 15), "Farmacia");
    Gasto deOctubre = new Gasto(5000.0, LocalDate.of(2026, 10, 2), "Gasto de octubre");
    alquiler.setUsuario(usuario);
    farmacia.setUsuario(usuario);
    deOctubre.setUsuario(usuario);

    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.persist(usuario);
      session.persist(alquiler);
      session.persist(farmacia);
      session.persist(deOctubre);
      transaction.commit();
    }

    mockMvc
      .perform(get("/home").param("mes", "2026-09").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(view().name("resumen"))
      .andExpect(content().string(containsString("Septiembre 2026")))
      .andExpect(content().string(containsString("$ 259.800")))
      .andExpect(content().string(containsString("Sin categoría")))
      .andExpect(content().string(containsString("100,0 %")))
      .andExpect(content().string(containsString("href=\"/home?mes=2026-08\"")))
      .andExpect(content().string(containsString("href=\"/home?mes=2026-10\"")));
  }

  @Test
  public void debeMostrarElAvisoDeMesSinGastosCuandoElMesNoTieneGastos() throws Exception {
    Usuario usuario = new Usuario();

    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.persist(usuario);
      transaction.commit();
    }

    mockMvc
      .perform(get("/home").param("mes", "2026-01").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Enero 2026")))
      .andExpect(content().string(containsString("Todavía no hay gastos este mes.")));
  }

  @Test
  public void debeRedirigirAlLoginCuandoSeEntraAlResumenSinSesion() throws Exception {
    mockMvc
      .perform(get("/home"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }
}
