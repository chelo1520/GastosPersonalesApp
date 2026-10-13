package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
    Categoria vivienda = categoria("Vivienda");
    Categoria salud = categoria("Salud");
    alquiler.setUsuario(usuario);
    farmacia.setUsuario(usuario);
    deOctubre.setUsuario(usuario);
    alquiler.setCategoria(vivienda);
    farmacia.setCategoria(salud);

    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.persist(usuario);
      session.persist(vivienda);
      session.persist(salud);
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
      // Historial: de abril a septiembre, con lo de septiembre en la última fila
      .andExpect(content().string(containsString("id=\"tabla-historial\"")))
      .andExpect(content().string(containsString("data-etiqueta=\"Abr 2026\"")))
      .andExpect(content().string(containsString("data-resto=\"259800.0\"")))
      .andExpect(content().string(containsString("$ 259.800")))
      // Presupuesto fijo de 550.000: quedan 290.200 disponibles
      .andExpect(content().string(containsString("$ 550.000")))
      .andExpect(content().string(containsString("$ 290.200")))
      .andExpect(content().string(containsString("Vivienda")))
      .andExpect(content().string(containsString("Salud")))
      .andExpect(content().string(containsString("96,2 %")))
      .andExpect(content().string(containsString("3,8 %")))
      .andExpect(content().string(containsString("href=\"/home?mes=2026-08\"")))
      .andExpect(content().string(containsString("href=\"/home?mes=2026-10\"")));
  }

  @Test
  public void debeRenderizarLosGastosHormigaDelMesYSusCategorias() throws Exception {
    Usuario usuario = new Usuario();
    Categoria cafe = categoria("Café y snacks");
    cafe.setEsencial(false);
    Categoria salud = categoria("Salud");

    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.persist(usuario);
      session.persist(cafe);
      session.persist(salud);
      // 4 cafés de 2.500 son hormiga; 4 gastos chicos de Salud no, porque es esencial
      for (int dia = 1; dia <= 4; dia++) {
        session.persist(gasto(2500.0, LocalDate.of(2025, 8, dia), cafe, usuario));
        session.persist(gasto(1000.0, LocalDate.of(2025, 8, dia), salud, usuario));
      }
      transaction.commit();
    }

    mockMvc
      .perform(get("/home").param("mes", "2025-08").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Gastos hormiga")))
      .andExpect(content().string(containsString("$ 10.000")))
      .andExpect(content().string(containsString("1,8</span> % del presupuesto")))
      .andExpect(content().string(containsString("Café y snacks")))
      // Agosto de 2025 ya cerró: no hay alertas
      .andExpect(content().string(not(containsString("app-alerta"))));
  }

  @Test
  public void debeMostrarLaAlertaDeAcumuladoYElBotonNoEsHormigaEnElMesEnCurso() throws Exception {
    LocalDate hoy = LocalDate.now();
    Usuario usuario = new Usuario();
    Categoria delivery = categoria("Delivery");
    delivery.setEsencial(false);

    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.persist(usuario);
      session.persist(delivery);
      // 4 pedidos de 8.000 = 32.000, el 5,8 % del presupuesto: alerta amarilla
      for (int i = 0; i < 4; i++) {
        session.persist(gasto(8000.0, hoy, delivery, usuario));
      }
      transaction.commit();
    }

    mockMvc
      .perform(get("/home").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("app-alerta")))
      .andExpect(
        content()
          .string(containsString("Llevás $32.000 en gastos hormiga, el 6% de tu presupuesto"))
      )
      .andExpect(
        content().string(containsString("/categorias/" + delivery.getId() + "/no-es-hormiga"))
      );
  }

  private Gasto gasto(Double importe, LocalDate fecha, Categoria categoria, Usuario usuario) {
    Gasto gasto = new Gasto(importe, fecha, "Gasto de prueba");
    gasto.setCategoria(categoria);
    gasto.setUsuario(usuario);
    return gasto;
  }

  private Categoria categoria(String nombre) {
    Categoria categoria = new Categoria();
    categoria.setNombre(nombre);
    categoria.setDescripcion("Categoría de prueba");
    return categoria;
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
      .andExpect(content().string(containsString("Todavía no hay gastos este mes.")))
      .andExpect(content().string(containsString("No hay gastos hormiga este mes.")));
  }

  @Test
  public void debeRedirigirAlLoginCuandoSeEntraAlResumenSinSesion() throws Exception {
    mockMvc
      .perform(get("/home"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }
}
