package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
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
      .andExpect(content().string(containsString("Lo que veo")))
      .andExpect(content().string(containsString("id=\"modal-confirmar\"")))
      .andExpect(content().string(not(containsString("confirm("))))
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

  @Test
  public void debeSugerirActualizarElPresupuestoYAplicarloDesdeElBotonCuandoQuedoAtrasDeLaInflacion()
    throws Exception {
    // El mismo presupuesto desde hace 6 meses con 2 % mensual de inflación → más del 5 % acumulado
    Categoria alimentacion = categoria("Alimentación");
    for (int i = 6; i >= 0; i--) {
      sessionFactory
        .getCurrentSession()
        .persist(new Presupuesto(usuario, alimentacion, YearMonth.now().minusMonths(i), 100000.0));
    }

    String pagina = mockMvc
      .perform(get("/asistente").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Tu presupuesto de Alimentación ($ 100.000)")))
      .andExpect(content().string(containsString("Actualizar presupuesto")))
      .andReturn()
      .getResponse()
      .getContentAsString();
    String id = pagina.replaceAll("(?s).*?/asistente/aplicar/(\\d+).*", "$1");

    mockMvc
      .perform(
        post("/asistente/aplicar/" + id).param("periodo", "6").sessionAttr("USUARIO", usuario)
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/asistente?periodo=6"));

    mockMvc
      .perform(get("/asistente").sessionAttr("USUARIO", usuario))
      .andExpect(content().string(not(containsString("Actualizar presupuesto"))));
    mockMvc
      .perform(get("/presupuestos").sessionAttr("USUARIO", usuario))
      .andExpect(content().string(not(containsString("100.000"))));
  }

  @Test
  public void debeOcultarElConsejoCuandoSeDescartaDesdeElBoton() throws Exception {
    Categoria alimentacion = categoria("Alimentación");
    for (int i = 6; i >= 0; i--) {
      sessionFactory
        .getCurrentSession()
        .persist(new Presupuesto(usuario, alimentacion, YearMonth.now().minusMonths(i), 100000.0));
    }
    String pagina = mockMvc
      .perform(get("/asistente").sessionAttr("USUARIO", usuario))
      .andReturn()
      .getResponse()
      .getContentAsString();
    String id = pagina.replaceAll("(?s).*?/asistente/descartar/(\\d+).*", "$1");

    mockMvc
      .perform(post("/asistente/descartar/" + id).sessionAttr("USUARIO", usuario))
      .andExpect(status().is3xxRedirection());

    mockMvc
      .perform(get("/asistente").sessionAttr("USUARIO", usuario))
      .andExpect(content().string(not(containsString("Descartar"))));
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
