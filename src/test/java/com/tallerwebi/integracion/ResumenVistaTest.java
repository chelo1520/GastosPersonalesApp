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

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
@Transactional
@Rollback
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

    persistir(usuario, vivienda, salud, alquiler, farmacia, deOctubre);
    // Solo Vivienda tiene presupuesto: 250.000 de 260.000 → 96 % usado, amarillo
    persistir(new Presupuesto(usuario, vivienda, YearMonth.of(2026, 9), 260000.0));

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
      // Presupuesto del mes = suma de los presupuestos por categoría: quedan 200 disponibles
      .andExpect(content().string(containsString("$ 260.000")))
      .andExpect(content().string(containsString("$ 200")))
      .andExpect(content().string(containsString("Vivienda")))
      .andExpect(content().string(containsString("Salud")))
      .andExpect(content().string(containsString("96,2 %")))
      .andExpect(content().string(containsString("3,8 %")))
      .andExpect(content().string(containsString("app-estado is-warn")))
      .andExpect(content().string(containsString("Sin presupuesto")))
      .andExpect(content().string(containsString("href=\"/home?mes=2026-08\"")))
      .andExpect(content().string(containsString("href=\"/home?mes=2026-10\"")));
  }

  @Test
  public void debeMostrarElCheckEsHormigaEnLaTablaDeCategoriasMenosEnLasFijas() throws Exception {
    Usuario usuario = new Usuario();
    Categoria kiosco = categoria("Kiosco");
    kiosco.setEsencial(false);
    Categoria otros = categoria("Otros");
    Categoria salud = categoria("Salud");
    salud.setFija(true);
    persistir(usuario, kiosco, otros, salud);
    persistir(gasto(1800.0, LocalDate.of(2026, 9, 3), kiosco, usuario));
    persistir(gasto(5000.0, LocalDate.of(2026, 9, 4), otros, usuario));
    persistir(gasto(9800.0, LocalDate.of(2026, 9, 5), salud, usuario));

    mockMvc
      .perform(get("/home").param("mes", "2026-09").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("¿Es hormiga?")))
      // Kiosco es hormiga: tildado, y al destildarlo pasa a esencial; se vuelve al mismo mes
      .andExpect(
        content()
          .string(
            containsString("/categorias/" + kiosco.getId() + "/no-es-hormiga?volver=/home?mes")
          )
      )
      .andExpect(content().string(containsString("Destildá para marcar Kiosco como esencial")))
      // Otros es esencial: sin tildar, y al tildarlo pasa a hormiga
      .andExpect(
        content()
          .string(containsString("/categorias/" + otros.getId() + "/es-hormiga?volver=/home?mes"))
      )
      .andExpect(content().string(containsString("Tildá para marcar Otros como prescindible")))
      // Salud es fija: siempre esencial, no se puede tildar
      .andExpect(content().string(not(containsString("/categorias/" + salud.getId() + "/"))))
      .andExpect(content().string(containsString("Categoría fija: siempre es esencial")));
  }

  @Test
  public void debeRenderizarLosGastosHormigaDelMesYSusCategorias() throws Exception {
    Usuario usuario = new Usuario();
    Categoria cafe = categoria("Café y snacks");
    cafe.setEsencial(false);
    Categoria salud = categoria("Salud");
    persistir(usuario, cafe, salud);
    // Presupuesto del mes: 550.000, así "chico" es hasta 11.000
    persistir(new Presupuesto(usuario, salud, YearMonth.of(2025, 8), 550000.0));
    // 4 cafés de 2.500 son hormiga; 4 gastos chicos de Salud no, porque es esencial
    for (int dia = 1; dia <= 4; dia++) {
      persistir(gasto(2500.0, LocalDate.of(2025, 8, dia), cafe, usuario));
      persistir(gasto(1000.0, LocalDate.of(2025, 8, dia), salud, usuario));
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
    Categoria vivienda = categoria("Vivienda");
    persistir(usuario, delivery, vivienda);
    persistir(new Presupuesto(usuario, vivienda, YearMonth.from(hoy), 550000.0));
    // 4 pedidos de 8.000 = 32.000, el 5,8 % del presupuesto: alerta amarilla
    for (int i = 0; i < 4; i++) {
      persistir(gasto(8000.0, hoy, delivery, usuario));
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

  @Test
  public void debePedirDefinirElPresupuestoParaCalcularLosGastosHormigaCuandoElMesNoTiene()
    throws Exception {
    Usuario usuario = new Usuario();
    Categoria cafe = categoria("Café y snacks");
    cafe.setEsencial(false);
    persistir(usuario, cafe);
    for (int dia = 1; dia <= 4; dia++) {
      persistir(gasto(2500.0, LocalDate.of(2025, 8, dia), cafe, usuario));
    }

    mockMvc
      .perform(get("/home").param("mes", "2025-08").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Definí el presupuesto del mes")))
      .andExpect(content().string(not(containsString("No hay gastos hormiga este mes."))));
  }

  private Gasto gasto(Double importe, LocalDate fecha, Categoria categoria, Usuario usuario) {
    Gasto gasto = new Gasto(importe, fecha, "Gasto de prueba");
    gasto.setCategoria(categoria);
    gasto.setUsuario(usuario);
    return gasto;
  }

  @Test
  public void debeMostrarElAvisoDeMesSinGastosCuandoElMesNoTieneGastos() throws Exception {
    Usuario usuario = new Usuario();
    persistir(usuario);

    mockMvc
      .perform(get("/home").param("mes", "2026-01").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Enero 2026")))
      .andExpect(content().string(containsString("Todavía no hay gastos este mes.")))
      .andExpect(content().string(containsString("Definí el presupuesto del mes")));
  }

  @Test
  public void debeRedirigirAlLoginCuandoSeEntraAlResumenSinSesion() throws Exception {
    mockMvc
      .perform(get("/home"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  private void persistir(Object... entidades) {
    for (Object entidad : entidades) {
      sessionFactory.getCurrentSession().persist(entidad);
    }
  }

  private Categoria categoria(String nombre) {
    Categoria categoria = new Categoria();
    categoria.setNombre(nombre);
    categoria.setDescripcion("Categoría de prueba");
    return categoria;
  }
}
