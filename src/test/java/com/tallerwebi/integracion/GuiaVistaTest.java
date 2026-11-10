package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDate;
import java.time.YearMonth;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
public class GuiaVistaTest {

  @Autowired
  private WebApplicationContext contexto;

  @Autowired
  private SessionFactory sessionFactory;

  private MockMvc mockMvc;
  private MockHttpSession sesion;
  private Usuario usuario;

  @BeforeEach
  void preparar() {
    mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    usuario = new Usuario();
    usuario.setEmail("guia@ejemplo.com");
    usuario.setGuiaPaso(0);
    sessionFactory.getCurrentSession().persist(usuario);
    sesion = new MockHttpSession();
    sesion.setAttribute("USUARIO", usuario);
  }

  @Test
  void posponeLaBienvenidaSoloEnLaSesionActual() throws Exception {
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("id=\"guia-bienvenida\"")));
    mockMvc.perform(post("/guia/pausar").session(sesion)).andExpect(redirectedUrl("/home"));
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(content().string(not(containsString("id=\"guia-bienvenida\""))));

    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    Usuario guardado = sessionFactory.getCurrentSession().find(Usuario.class, usuario.getId());
    org.junit.jupiter.api.Assertions.assertEquals(0, guardado.getGuiaPaso());
    sesion = new MockHttpSession();
    sesion.setAttribute("USUARIO", guardado);
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(content().string(containsString("id=\"guia-bienvenida\"")));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/perfil"));
    mockMvc
      .perform(get("/perfil").session(sesion))
      .andExpect(content().string(containsString("Paso 1 de 6")))
      .andExpect(content().string(containsString("id=\"guia-destino-perfil\"")))
      .andExpect(content().string(not(containsString("Guía de Gastitos"))))
      .andExpect(content().string(not(containsString("Omitir paso"))))
      .andExpect(content().string(not(containsString("Pausar guía"))))
      .andExpect(content().string(not(containsString("Ir a mi perfil"))));
  }

  @Test
  void unaCuentaAnteriorNoRecibeLaBienvenidaAutomaticamente() throws Exception {
    usuario.setGuiaPaso(null);
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(status().isOk())
      .andExpect(content().string(not(containsString("id=\"guia-bienvenida\""))));
  }

  @Test
  void unaGuiaFinalizadaAntesDeAgregarLosPasosNoVuelveAAparecer() throws Exception {
    usuario.setGuiaPaso(5);
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(status().isOk())
      .andExpect(content().string(not(containsString("id=\"guia-pasos\""))))
      .andExpect(content().string(not(containsString("id=\"guia-bienvenida\""))));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(status().isBadRequest());
  }

  @Test
  void permiteAvanzarCuandoSeGuardanPresupuestoCategoriaYGasto() throws Exception {
    Categoria categoria = new Categoria();
    categoria.setNombre("Transporte");
    sessionFactory.getCurrentSession().persist(categoria);
    String mes = YearMonth.now().toString();

    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/perfil"));
    mockMvc
      .perform(
        post("/perfil/presupuesto")
          .session(sesion)
          .param("ingresoMensual", "-1")
          .param("porcentaje", "100")
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Paso 1 de 6")))
      .andExpect(content().string(containsString("El ingreso debe ser mayor a cero")));
    org.junit.jupiter.api.Assertions.assertEquals(1, usuario.getGuiaPaso());
    mockMvc
      .perform(
        post("/perfil/presupuesto")
          .session(sesion)
          .param("ingresoMensual", "1000")
          .param("porcentaje", "100")
      )
      .andExpect(redirectedUrl("/presupuestos"));
    org.junit.jupiter.api.Assertions.assertEquals(2, usuario.getGuiaPaso());
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    Usuario actualizado = sessionFactory.getCurrentSession().find(Usuario.class, usuario.getId());
    org.junit.jupiter.api.Assertions.assertEquals(2, actualizado.getGuiaPaso());
    org.junit.jupiter.api.Assertions.assertEquals(1000.0, actualizado.getIngresoMensual());
    mockMvc
      .perform(get("/presupuestos").session(sesion))
      .andExpect(content().string(containsString("Organizá tu presupuesto")));
    mockMvc
      .perform(
        post("/presupuestos")
          .session(sesion)
          .param("categoriaId", categoria.getId().toString())
          .param("mes", mes)
          .param("monto", "500")
      )
      .andExpect(redirectedUrl("/presupuestos?mes=" + mes));
    mockMvc
      .perform(post("/guia/continuar").session(sesion))
      .andExpect(redirectedUrl("/registrar-gasto"));
    mockMvc
      .perform(get("/registrar-gasto").session(sesion))
      .andExpect(content().string(containsString("Registrá un gasto")));
    mockMvc
      .perform(
        post("/registrar-gasto")
          .session(sesion)
          .param("categoria.id", categoria.getId().toString())
          .param("importe", "100")
          .param("fecha", LocalDate.now().toString())
          .param("descripcion", "Viaje")
      )
      .andExpect(redirectedUrl("/mostrar-gastos"));
    mockMvc
      .perform(get("/mostrar-gastos").session(sesion))
      .andExpect(content().string(containsString("Paso 3 de 6")));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/home"));
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(content().string(containsString("Conocé tu resumen")));
    mockMvc
      .perform(post("/guia/continuar").session(sesion))
      .andExpect(redirectedUrl("/simulacion"));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/asistente"));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/home"));
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(content().string(not(containsString("id=\"guia-bienvenida\""))));
  }

  @Test
  void permiteFinalizarSinGuardarNingunDato() throws Exception {
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/perfil"));
    mockMvc
      .perform(post("/guia/continuar").session(sesion))
      .andExpect(redirectedUrl("/presupuestos"));
    mockMvc
      .perform(post("/guia/continuar").session(sesion))
      .andExpect(redirectedUrl("/registrar-gasto"));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/home"));
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(content().string(containsString("Paso 4 de 6")));
    mockMvc
      .perform(post("/guia/continuar").session(sesion))
      .andExpect(redirectedUrl("/simulacion"));
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    org.junit.jupiter.api.Assertions.assertEquals(
      6,
      sessionFactory.getCurrentSession().find(Usuario.class, usuario.getId()).getGuiaPaso()
    );
    mockMvc
      .perform(get("/simulacion").session(sesion))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Paso 5 de 6")))
      .andExpect(content().string(containsString("Explorá la simulación")))
      .andExpect(content().string(containsString("id=\"guia-destino-simulacion\"")));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/asistente"));
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    org.junit.jupiter.api.Assertions.assertEquals(
      7,
      sessionFactory.getCurrentSession().find(Usuario.class, usuario.getId()).getGuiaPaso()
    );
    mockMvc
      .perform(get("/asistente").session(sesion))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Paso 6 de 6")))
      .andExpect(content().string(containsString("Conocé el asistente")))
      .andExpect(content().string(containsString("id=\"guia-destino-asistente\"")))
      .andExpect(content().string(containsString("Finalizar")));
    mockMvc.perform(post("/guia/anterior").session(sesion)).andExpect(redirectedUrl("/simulacion"));
    mockMvc.perform(post("/guia/anterior").session(sesion)).andExpect(redirectedUrl("/home"));
    mockMvc
      .perform(post("/guia/continuar").session(sesion))
      .andExpect(redirectedUrl("/simulacion"));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/asistente"));
    mockMvc.perform(post("/guia/continuar").session(sesion)).andExpect(redirectedUrl("/home"));
    mockMvc
      .perform(get("/home").session(sesion))
      .andExpect(content().string(not(containsString("id=\"guia-pasos\""))));
    org.junit.jupiter.api.Assertions.assertEquals(5, usuario.getGuiaPaso());
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    org.junit.jupiter.api.Assertions.assertEquals(
      5,
      sessionFactory.getCurrentSession().find(Usuario.class, usuario.getId()).getGuiaPaso()
    );
  }
}
