package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDate;
import java.time.YearMonth;
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

/** La vista del asistente con los indicadores fijos de SpringWebTestConfig (sin red). */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
@Transactional
@Rollback
public class AsistenteVistaTest {

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
    usuario.setEmail("diego@unlam.edu.ar");
    usuario.setPassword("123");
    sessionFactory.getCurrentSession().persist(usuario);
  }

  @Test
  public void debeRedirigirAlLoginCuandoNoExisteUnaSesion() throws Exception {
    mockMvc
      .perform(get("/asistente"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  @Test
  public void debeMostrarElVeredictoYLaTablaCuandoHayDosMesesCerradosConGastos() throws Exception {
    Categoria alimentacion = categoria("Alimentación");
    YearMonth hace2 = YearMonth.now().minusMonths(2);
    YearMonth hace1 = YearMonth.now().minusMonths(1);
    gasto(100000.0, hace2.atDay(5), alimentacion);
    gasto(120000.0, hace1.atDay(5), alimentacion);

    mockMvc
      .perform(get("/asistente").param("periodo", "3").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(view().name("asistente"))
      .andExpect(content().string(containsString("Tu asistente")))
      .andExpect(content().string(containsString("tus gastos subieron 20,0 %")))
      .andExpect(content().string(containsString("Alimentación")))
      .andExpect(content().string(containsString("Consumís más")))
      .andExpect(content().string(containsString("ArgentinaDatos")));
  }

  @Test
  public void debeExplicarQueFaltanMesesCuandoElUsuarioNoTieneGastos() throws Exception {
    mockMvc
      .perform(get("/asistente").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Todavía no tengo dos meses cerrados")));
  }

  private Categoria categoria(String nombre) {
    Categoria categoria = new Categoria();
    categoria.setNombre(nombre);
    sessionFactory.getCurrentSession().persist(categoria);
    return categoria;
  }

  private void gasto(Double importe, LocalDate fecha, Categoria categoria) {
    Gasto gasto = new Gasto(importe, fecha, "Gasto de prueba");
    gasto.setUsuario(usuario);
    gasto.setCategoria(categoria);
    sessionFactory.getCurrentSession().persist(gasto);
  }
}
