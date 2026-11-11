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
      .andExpect(content().string(containsString("130.000")))
      .andExpect(content().string(containsString("$ 130.000")));
  }

  @Test
  public void debeMostrarGastadoDisponibleYElChipDeEstadoCuandoLaCategoriaTienePresupuestoYGastos()
    throws Exception {
    Gasto compra = new Gasto(118400.0, LocalDate.of(2026, 10, 5), "Compra del mes");
    compra.setUsuario(usuario);
    compra.setCategoria(alimentacion);
    sessionFactory.getCurrentSession().persist(compra);

    mockMvc
      .perform(
        post("/presupuestos")
          .sessionAttr("USUARIO", usuario)
          .param("categoriaId", alimentacion.getId().toString())
          .param("monto", "130000")
          .param("mes", "2026-10")
      )
      .andExpect(status().is3xxRedirection());

    mockMvc
      .perform(get("/presupuestos").param("mes", "2026-10").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("118.400")))
      .andExpect(content().string(containsString("11.600")))
      .andExpect(content().string(containsString("app-estado is-warn")))
      .andExpect(content().string(containsString("91 %")));
  }

  @Test
  public void debeMostrarElLapizYElFormularioDeEdicionEnLaFilaConElMontoActual() throws Exception {
    mockMvc
      .perform(
        post("/presupuestos")
          .sessionAttr("USUARIO", usuario)
          .param("categoriaId", alimentacion.getId().toString())
          .param("monto", "130000")
          .param("mes", "2026-10")
      )
      .andExpect(status().is3xxRedirection());

    mockMvc
      .perform(get("/presupuestos").param("mes", "2026-10").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("app-presupuesto-lapiz")))
      .andExpect(content().string(containsString("Editar el presupuesto de Alimentación")))
      .andExpect(content().string(containsString("class=\"app-presupuesto-editar\"")))
      .andExpect(content().string(containsString("name=\"tipo\"")))
      .andExpect(content().string(containsString("Porcentaje (%)")))
      .andExpect(
        content()
          .string(containsString("name=\"categoriaId\" value=\"" + alimentacion.getId() + "\""))
      )
      .andExpect(content().string(containsString("value=\"130000.0\"")));
  }

  @Test
  public void debeReemplazarElMontoCuandoSeEditaElPresupuestoDesdeLaFila() throws Exception {
    for (String monto : new String[] { "130000", "150000" }) {
      mockMvc
        .perform(
          post("/presupuestos")
            .sessionAttr("USUARIO", usuario)
            .param("categoriaId", alimentacion.getId().toString())
            .param("monto", monto)
            .param("mes", "2026-10")
        )
        .andExpect(status().is3xxRedirection());
    }

    mockMvc
      .perform(get("/presupuestos").param("mes", "2026-10").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("$ 150.000")));
  }

  @Test
  public void debeOfrecerOcultarLasCategoriasSinPresupuesto() throws Exception {
    mockMvc
      .perform(get("/presupuestos").param("mes", "2026-10").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("id=\"ocultar-sin-presupuesto\"")))
      .andExpect(content().string(containsString("Ocultar categorías sin presupuesto")))
      .andExpect(content().string(containsString("app-presupuesto-sin-definir")))
      .andExpect(content().string(containsString("/js/presupuestos.js")));
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

  @Test
  public void debeRecalcularElPorcentajeAlCambiarElTotalDelMesYAlVolverAPesos() throws Exception {
    String mes = YearMonth.now().toString();
    usuario.setIngresoMensual(1000.0);
    usuario.setPorcentajePresupuesto(100.0);
    sessionFactory.getCurrentSession().flush();

    mockMvc
      .perform(
        post("/presupuestos")
          .sessionAttr("USUARIO", usuario)
          .param("categoriaId", alimentacion.getId().toString())
          .param("tipo", "porcentaje")
          .param("valor", "25")
          .param("mes", mes)
      )
      .andExpect(redirectedUrl("/presupuestos?mes=" + mes));
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    Presupuesto guardado = sessionFactory
      .getCurrentSession()
      .createQuery("FROM Presupuesto WHERE usuario.id = :id", Presupuesto.class)
      .setParameter("id", usuario.getId())
      .getSingleResult();
    org.junit.jupiter.api.Assertions.assertEquals(25.0, guardado.getPorcentaje());

    mockMvc
      .perform(
        post("/perfil/presupuesto/mes")
          .sessionAttr("USUARIO", usuario)
          .param("mes", mes)
          .param("monto", "2000")
      )
      .andExpect(redirectedUrl("/perfil?mesGuardado&mes=" + mes));
    mockMvc
      .perform(get("/presupuestos").param("mes", mes).sessionAttr("USUARIO", usuario))
      .andExpect(content().string(containsString("25.0 % del total")))
      .andExpect(content().string(containsString("$ 500")));

    mockMvc
      .perform(
        post("/presupuestos")
          .sessionAttr("USUARIO", usuario)
          .param("categoriaId", alimentacion.getId().toString())
          .param("tipo", "pesos")
          .param("valor", "300")
          .param("mes", mes)
      )
      .andExpect(redirectedUrl("/presupuestos?mes=" + mes));
    mockMvc
      .perform(
        post("/perfil/presupuesto/mes")
          .sessionAttr("USUARIO", usuario)
          .param("mes", mes)
          .param("monto", "4000")
      )
      .andExpect(redirectedUrl("/perfil?mesGuardado&mes=" + mes));
    mockMvc
      .perform(get("/presupuestos").param("mes", mes).sessionAttr("USUARIO", usuario))
      .andExpect(content().string(containsString("$ 300")));
  }

  @Test
  public void debeMostrarErrorSiNoHayTotalOElPorcentajeEsInvalido() throws Exception {
    mockMvc
      .perform(
        post("/presupuestos")
          .sessionAttr("USUARIO", usuario)
          .param("categoriaId", alimentacion.getId().toString())
          .param("tipo", "porcentaje")
          .param("valor", "25")
          .param("mes", "2026-10")
      )
      .andExpect(content().string(containsString("Definí primero un presupuesto total")));
    usuario.setIngresoMensual(1000.0);
    for (String valor : new String[] { "0", "101" }) {
      mockMvc
        .perform(
          post("/presupuestos")
            .sessionAttr("USUARIO", usuario)
            .param("categoriaId", alimentacion.getId().toString())
            .param("tipo", "porcentaje")
            .param("valor", valor)
            .param("mes", "2026-10")
        )
        .andExpect(content().string(containsString("El porcentaje debe ser mayor a cero")));
    }
  }

  @Test
  public void debeRecalcularSegunElPerfilYRespetarElTotalPropioDeOtroMes() throws Exception {
    String mes = YearMonth.now().toString();
    String otroMes = YearMonth.now().plusMonths(1).toString();
    usuario.setIngresoMensual(1000.0);
    usuario.setPorcentajePresupuesto(100.0);
    mockMvc
      .perform(
        post("/perfil/presupuesto/mes")
          .sessionAttr("USUARIO", usuario)
          .param("mes", otroMes)
          .param("monto", "3000")
      )
      .andExpect(status().is3xxRedirection());
    for (String periodo : new String[] { mes, otroMes }) {
      mockMvc
        .perform(
          post("/presupuestos")
            .sessionAttr("USUARIO", usuario)
            .param("categoriaId", alimentacion.getId().toString())
            .param("tipo", "porcentaje")
            .param("valor", "25")
            .param("mes", periodo)
        )
        .andExpect(status().is3xxRedirection());
    }

    mockMvc
      .perform(
        post("/perfil/presupuesto")
          .sessionAttr("USUARIO", usuario)
          .param("ingresoMensual", "2000")
          .param("porcentaje", "100")
      )
      .andExpect(redirectedUrl("/perfil"));
    mockMvc
      .perform(get("/presupuestos").param("mes", mes).sessionAttr("USUARIO", usuario))
      .andExpect(content().string(containsString("$ 500")));
    mockMvc
      .perform(get("/presupuestos").param("mes", otroMes).sessionAttr("USUARIO", usuario))
      .andExpect(content().string(containsString("$ 750")));
    mockMvc
      .perform(get("/home").param("mes", mes).sessionAttr("USUARIO", usuario))
      .andExpect(content().string(containsString("500")));
  }
}
