package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
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
public class PresupuestosVistaTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private SessionFactory sessionFactory;

  private MockMvc mockMvc;
  private Usuario usuario;
  private Categoria alimentacion;

  @BeforeEach
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    usuario = new Usuario();
    usuario.setEmail("diego@unlam.edu.ar");
    usuario.setPassword("123");
    sessionFactory.getCurrentSession().persist(usuario);

    alimentacion = new Categoria();
    alimentacion.setNombre("Alimentación");
    sessionFactory.getCurrentSession().persist(alimentacion);
  }

  @Test
  public void debeRedirigirAlLoginCuandoNoExisteUnaSesion() throws Exception {
    mockMvc
      .perform(get("/presupuestos"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  @Test
  public void debeMostrarSinPresupuestoYTotalCeroCuandoElUsuarioNoDefinioNingunoEnElMes()
    throws Exception {
    mockMvc
      .perform(get("/presupuestos").param("mes", "2026-10").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(view().name("presupuestos"))
      .andExpect(content().string(containsString("Octubre 2026")))
      .andExpect(content().string(containsString("Alimentación")))
      .andExpect(content().string(containsString("Sin presupuesto")))
      .andExpect(content().string(containsString("$ 0")));
  }

  @Test
  public void debeMostrarElMontoDefinidoCuandoSeGuardaUnPresupuestoDesdeElFormulario()
    throws Exception {
    mockMvc
      .perform(
        post("/presupuestos")
          .sessionAttr("USUARIO", usuario)
          .param("categoriaId", alimentacion.getId().toString())
          .param("monto", "130000")
          .param("mes", "2026-10")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/presupuestos?mes=2026-10"));

    mockMvc
      .perform(get("/presupuestos").param("mes", "2026-10").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("130.000")));
  }

  @Test
  public void debeVolverAlFormularioConErrorSinGuardarCuandoElMontoEsCero() throws Exception {
    mockMvc
      .perform(
        post("/presupuestos")
          .sessionAttr("USUARIO", usuario)
          .param("categoriaId", alimentacion.getId().toString())
          .param("monto", "0")
          .param("mes", "2026-10")
      )
      .andExpect(status().isOk())
      .andExpect(view().name("presupuestos"))
      .andExpect(content().string(containsString("El monto debe ser mayor a cero")))
      .andExpect(content().string(containsString("Sin presupuesto")));
  }
}
