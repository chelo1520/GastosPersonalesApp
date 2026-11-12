package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.NombreDeMes;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.presupuesto.PresupuestoTotal;
import com.tallerwebi.dominio.simulacion.EstadoSimulacion;
import com.tallerwebi.dominio.simulacion.ServicioSimulacion;
import com.tallerwebi.infraestructura.RepositorioUsuarioImpl;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.YearMonth;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
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
public class SimulacionVistaTest {

  @Autowired
  private WebApplicationContext contexto;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private ServicioSimulacion servicioSimulacion;

  private MockMvc mockMvc;
  private Usuario usuario;

  @BeforeEach
  void preparar() {
    mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    usuario = new Usuario();
    usuario.setEmail("simulacion@ejemplo.com");
    usuario.setIngresoMensual(500000.0);
    usuario.setPorcentajePresupuesto(100.0);
    sessionFactory.getCurrentSession().persist(usuario);
  }

  @Test
  void permiteRecuperarSimulacionesPasadasGuardadasPeroNoCrearOtras() throws Exception {
    YearMonth base = YearMonth.now().minusMonths(30);
    YearMonth pasado = YearMonth.now().minusMonths(1);
    servicioSimulacion.guardar(
      usuario,
      base,
      pasado,
      new EstadoSimulacion(java.util.List.of(), java.util.List.of(), "Guardado antiguo", "", "", "")
    );
    mockMvc
      .perform(
        get("/simulacion")
          .sessionAttr("USUARIO", usuario)
          .param("mesBase", base.toString())
          .param("mesSimulado", pasado.toString())
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Guardado antiguo")))
      .andExpect(content().string(containsString("selected=\"selected\">" + nombre(base))))
      .andExpect(content().string(containsString("selected=\"selected\">" + nombre(pasado))));
    mockMvc
      .perform(
        get("/simulacion")
          .sessionAttr("USUARIO", usuario)
          .param("mesBase", base.toString())
          .param("mesSimulado", pasado.minusMonths(1).toString())
      )
      .andExpect(status().isBadRequest());
    String estado =
      """
      {"excluidos":[],"agregados":[],"descripcionExtra":"Actualizado",
      "montoExtra":"","descripcionOrdinario":"","montoOrdinario":""}
      """;
    mockMvc
      .perform(
        post("/simulacion/guardar")
          .sessionAttr("USUARIO", usuario)
          .param("mesBase", base.toString())
          .param("mesSimulado", pasado.toString())
          .contentType(MediaType.APPLICATION_JSON)
          .content(estado)
      )
      .andExpect(status().isNoContent());
    mockMvc
      .perform(
        post("/simulacion/guardar")
          .sessionAttr("USUARIO", usuario)
          .param("mesBase", base.toString())
          .param("mesSimulado", pasado.minusMonths(1).toString())
          .contentType(MediaType.APPLICATION_JSON)
          .content(estado)
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void alEliminarCuentaSeEliminanSusSimulaciones() {
    YearMonth mes = YearMonth.now();
    servicioSimulacion.guardar(usuario, mes, mes, EstadoSimulacion.vacio());
    Long id = usuario.getId();
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    new RepositorioUsuarioImpl(sessionFactory).eliminar(usuario);
    sessionFactory.getCurrentSession().flush();
    assertTrue(
      sessionFactory
        .getCurrentSession()
        .createQuery("from SimulacionGuardada where usuario.id = :id", Object.class)
        .setParameter("id", id)
        .getResultList()
        .isEmpty()
    );
  }

  @Test
  void guardaYRecuperaPorUsuarioYMesesConGastosRealesActualizados() throws Exception {
    YearMonth base = YearMonth.now().minusMonths(1);
    YearMonth simulado = YearMonth.now().plusMonths(1);
    Gasto gasto = new Gasto(10.0, base.atDay(1), "Original");
    gasto.setUsuario(usuario);
    sessionFactory.getCurrentSession().persist(gasto);
    String estado =
      """
      {"excluidos":[%d],"agregados":[{"descripcion":"Viaje","importe":50,"tipo":"extraordinario","incluido":false}],
       "descripcionExtra":"Borrador","montoExtra":"25","descripcionOrdinario":"","montoOrdinario":""}
      """.formatted(gasto.getId());
    MockHttpSession sesion = new MockHttpSession();
    sesion.setAttribute("USUARIO", usuario);
    mockMvc
      .perform(
        post("/simulacion/guardar")
          .session(sesion)
          .param("mesBase", base.toString())
          .param("mesSimulado", simulado.toString())
          .contentType(MediaType.APPLICATION_JSON)
          .content(estado)
      )
      .andExpect(status().isNoContent());
    gasto.setDescripcion("Original actualizado");
    Gasto nuevo = new Gasto(20.0, base.atDay(2), "Nuevo real");
    nuevo.setUsuario(usuario);
    sessionFactory.getCurrentSession().persist(nuevo);
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    mockMvc
      .perform(
        get("/simulacion")
          .session(sesion)
          .param("mesBase", base.toString())
          .param("mesSimulado", simulado.toString())
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Original actualizado")))
      .andExpect(content().string(containsString("Nuevo real")))
      .andExpect(content().string(containsString("Viaje")))
      .andExpect(content().string(containsString("value=\"Borrador\"")))
      .andExpect(
        content()
          .string(
            matchesPattern(
              "(?s).*data-id=\"" +
              gasto.getId() +
              "\"[^>]*>\\s*<label[^>]*>\\s*<input class=\"app-sim-check\" type=\"checkbox\">.*"
            )
          )
      )
      .andExpect(
        content()
          .string(
            matchesPattern(
              "(?s).*data-descripcion=\"Viaje\"[^>]*>\\s*<label[^>]*>\\s*<input class=\"app-sim-check\" type=\"checkbox\">.*"
            )
          )
      );
    mockMvc
      .perform(get("/simulacion").session(sesion))
      .andExpect(content().string(containsString("Viaje")))
      .andExpect(content().string(containsString("selected=\"selected\">" + nombre(base))));
    mockMvc
      .perform(
        get("/simulacion")
          .session(sesion)
          .param("mesBase", base.toString())
          .param("mesSimulado", YearMonth.now().toString())
      )
      .andExpect(content().string(not(containsString("Viaje"))));
    Usuario otro = new Usuario();
    otro.setEmail("distinto@ejemplo.com");
    sessionFactory.getCurrentSession().persist(otro);
    mockMvc
      .perform(
        get("/simulacion")
          .sessionAttr("USUARIO", otro)
          .param("mesBase", base.toString())
          .param("mesSimulado", simulado.toString())
      )
      .andExpect(content().string(not(containsString("Viaje"))));
    mockMvc
      .perform(
        post("/simulacion/guardar")
          .param("mesBase", base.toString())
          .param("mesSimulado", simulado.toString())
          .contentType(MediaType.APPLICATION_JSON)
          .content(estado)
      )
      .andExpect(status().isUnauthorized());
  }

  @Test
  void rechazaEstadosInvalidosSinSobrescribirLosGuardados() throws Exception {
    YearMonth base = YearMonth.now();
    String valido =
      """
      {"excluidos":[],"agregados":[],"descripcionExtra":"Conservado",
      "montoExtra":"","descripcionOrdinario":"","montoOrdinario":""}
      """;
    mockMvc
      .perform(
        post("/simulacion/guardar")
          .sessionAttr("USUARIO", usuario)
          .param("mesBase", base.toString())
          .param("mesSimulado", base.toString())
          .contentType(MediaType.APPLICATION_JSON)
          .content(valido)
      )
      .andExpect(status().isNoContent());
    mockMvc
      .perform(
        post("/simulacion/guardar")
          .sessionAttr("USUARIO", usuario)
          .param("mesBase", base.toString())
          .param("mesSimulado", base.toString())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"excluidos\":null}")
      )
      .andExpect(status().isBadRequest());
    mockMvc
      .perform(
        get("/simulacion")
          .sessionAttr("USUARIO", usuario)
          .param("mesBase", base.toString())
          .param("mesSimulado", base.toString())
      )
      .andExpect(content().string(containsString("Conservado")));
  }

  @Test
  void permiteElegirLosDosMesesYUsaLosGastosYElPresupuestoCorrespondientes() throws Exception {
    YearMonth mesBase = YearMonth.now().minusMonths(2);
    YearMonth mesSimulado = YearMonth.now().plusMonths(2);
    Categoria categoria = new Categoria();
    categoria.setNombre("Transporte");
    sessionFactory.getCurrentSession().persist(categoria);
    Gasto gastoBase = new Gasto(12000.0, mesBase.atDay(10), "Viaje base");
    gastoBase.setUsuario(usuario);
    gastoBase.setCategoria(categoria);
    sessionFactory.getCurrentSession().persist(gastoBase);
    Gasto otroMes = new Gasto(9000.0, mesBase.plusMonths(1).atDay(10), "Viaje de otro mes");
    otroMes.setUsuario(usuario);
    otroMes.setCategoria(categoria);
    sessionFactory.getCurrentSession().persist(otroMes);
    sessionFactory
      .getCurrentSession()
      .persist(new PresupuestoTotal(usuario, mesSimulado, 700000.0));

    mockMvc
      .perform(
        get("/simulacion")
          .param("mesBase", mesBase.toString())
          .param("mesSimulado", mesSimulado.toString())
          .sessionAttr("USUARIO", usuario)
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(NombreDeMes.de(mesBase))))
      .andExpect(content().string(containsString(NombreDeMes.de(mesSimulado))))
      .andExpect(
        content()
          .string(
            matchesPattern(
              "(?s).*<h1[^>]*>Simulación · <span>" + nombre(mesSimulado) + "</span></h1>.*"
            )
          )
      )
      .andExpect(
        content()
          .string(
            matchesPattern(
              "(?s).*Gastos de <span>" +
              nombre(mesBase) +
              "</span>\\s+como base para simular <span>" +
              nombre(mesSimulado) +
              "</span>.*"
            )
          )
      )
      .andExpect(content().string(containsString("Viaje base")))
      .andExpect(content().string(not(containsString("Viaje de otro mes"))))
      .andExpect(content().string(containsString("Transporte")))
      .andExpect(content().string(containsString("data-presupuesto=\"700000.0\"")))
      .andExpect(content().string(containsString("name=\"mesBase\"")))
      .andExpect(content().string(containsString("name=\"mesSimulado\"")))
      .andExpect(content().string(containsString("value=\"" + mesBase + "\"")))
      .andExpect(content().string(containsString("selected=\"selected\">" + nombre(mesBase))))
      .andExpect(content().string(containsString("value=\"" + mesSimulado + "\"")))
      .andExpect(content().string(containsString("selected=\"selected\">" + nombre(mesSimulado))));
  }

  @Test
  void conservaEnLosSelectoresLosMesesValidosFueraDelRangoHabitual() throws Exception {
    YearMonth base = YearMonth.now().minusMonths(30);
    YearMonth simulado = YearMonth.now().plusMonths(30);
    mockMvc
      .perform(
        get("/simulacion")
          .param("mesBase", base.toString())
          .param("mesSimulado", simulado.toString())
          .sessionAttr("USUARIO", usuario)
      )
      .andExpect(status().isOk())
      .andExpect(
        content()
          .string(
            matchesPattern(
              "(?s).*value=\"" +
              base +
              "\"\\s+selected=\"selected\">" +
              nombre(base) +
              "</option>.*"
            )
          )
      )
      .andExpect(
        content()
          .string(
            matchesPattern(
              "(?s).*value=\"" +
              simulado +
              "\"\\s+selected=\"selected\">" +
              nombre(simulado) +
              "</option>.*"
            )
          )
      );
  }

  @Test
  void limitaLosSelectoresYRechazaMesesFueraDeLaRegla() throws Exception {
    YearMonth actual = YearMonth.now();
    String html = mockMvc
      .perform(get("/simulacion").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
    String selectorBase = html.substring(
      html.indexOf("id=\"mes-base\""),
      html.indexOf("</select>", html.indexOf("id=\"mes-base\""))
    );
    String selectorSimulado = html.substring(
      html.indexOf("id=\"mes-simulado\""),
      html.indexOf("</select>", html.indexOf("id=\"mes-simulado\""))
    );
    assertTrue(selectorBase.contains("value=\"" + actual + "\""));
    assertFalse(selectorBase.contains("value=\"" + actual.plusMonths(1) + "\""));
    assertTrue(selectorSimulado.contains("value=\"" + actual + "\""));
    assertFalse(selectorSimulado.contains("value=\"" + actual.minusMonths(1) + "\""));
    mockMvc
      .perform(
        get("/simulacion")
          .param("mesBase", actual.plusMonths(1).toString())
          .sessionAttr("USUARIO", usuario)
      )
      .andExpect(status().isBadRequest());
    mockMvc
      .perform(
        get("/simulacion")
          .param("mesSimulado", actual.minusMonths(1).toString())
          .sessionAttr("USUARIO", usuario)
      )
      .andExpect(status().isBadRequest());
    mockMvc
      .perform(
        get("/simulacion")
          .param("mesBase", actual.toString())
          .param("mesSimulado", actual.toString())
          .sessionAttr("USUARIO", usuario)
      )
      .andExpect(status().isOk());
  }

  @Test
  void cambiarMesBaseCargaSoloLosGastosPropiosDeEseMesInclusoEnLosLimites() throws Exception {
    YearMonth base = YearMonth.now().minusMonths(1);
    Usuario otroUsuario = new Usuario();
    otroUsuario.setEmail("otro@ejemplo.com");
    sessionFactory.getCurrentSession().persist(otroUsuario);
    for (Gasto gasto : new Gasto[] {
      new Gasto(10.0, base.atDay(1), "Inicio base"),
      new Gasto(20.0, base.atEndOfMonth(), "Fin base"),
      new Gasto(30.0, base.plusMonths(1).atDay(1), "Mes actual"),
    }) {
      gasto.setUsuario(usuario);
      sessionFactory.getCurrentSession().persist(gasto);
    }
    Gasto ajeno = new Gasto(40.0, base.atDay(1), "Gasto ajeno");
    ajeno.setUsuario(otroUsuario);
    sessionFactory.getCurrentSession().persist(ajeno);
    mockMvc
      .perform(
        get("/simulacion")
          .param("mesBase", base.toString())
          .param("mesSimulado", YearMonth.now().toString())
          .sessionAttr("USUARIO", usuario)
      )
      .andExpect(content().string(containsString("Inicio base")))
      .andExpect(content().string(containsString("Fin base")))
      .andExpect(content().string(not(containsString("Mes actual"))))
      .andExpect(content().string(not(containsString("Gasto ajeno"))));
    mockMvc
      .perform(
        get("/simulacion")
          .param("mesBase", YearMonth.now().toString())
          .param("mesSimulado", YearMonth.now().toString())
          .sessionAttr("USUARIO", usuario)
      )
      .andExpect(content().string(containsString("Mes actual")))
      .andExpect(content().string(not(containsString("Inicio base"))))
      .andExpect(content().string(not(containsString("Fin base"))));
  }

  @Test
  void muestraMesesPredeterminadosYRechazaParametrosInvalidos() throws Exception {
    YearMonth actual = YearMonth.now();
    mockMvc
      .perform(get("/simulacion").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(
        content().string(containsString("selected=\"selected\">" + nombre(actual.minusMonths(1))))
      )
      .andExpect(
        content().string(containsString("selected=\"selected\">" + nombre(actual.plusMonths(1))))
      );
    mockMvc
      .perform(get("/simulacion").param("mesBase", "noviembre").sessionAttr("USUARIO", usuario))
      .andExpect(status().isBadRequest());
  }

  private String nombre(YearMonth mes) {
    return com.tallerwebi.dominio.NombreDeMes.de(mes);
  }
}
