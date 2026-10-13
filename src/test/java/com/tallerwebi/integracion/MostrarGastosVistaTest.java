package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDate;
import java.time.YearMonth;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
  public void debeMostrarEsHormigaEnCategoriasEsencialesYNoEsHormigaEnPrescindiblesConSuLeyenda()
    throws Exception {
    Usuario usuario = new Usuario();
    Categoria otros = categoria("Otros", true, false);
    Categoria kiosco = categoria("Kiosco", false, false);
    Categoria salud = categoria("Salud", true, true);
    sessionFactory.getCurrentSession().persist(usuario);
    sessionFactory.getCurrentSession().persist(otros);
    sessionFactory.getCurrentSession().persist(kiosco);
    sessionFactory.getCurrentSession().persist(salud);
    persistirGasto(1800.0, "Golosina", kiosco, usuario);
    persistirGasto(5000.0, "Regalo", otros, usuario);
    persistirGasto(9800.0, "Farmacia", salud, usuario);

    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc
      .perform(get("/mostrar-gastos").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(
        content()
          .string(
            containsString("/categorias/" + otros.getId() + "/es-hormiga?volver=/mostrar-gastos")
          )
      )
      .andExpect(content().string(containsString("Marca Otros como prescindible")))
      // Salud es fija: se muestra la categoría, pero sin botón para marcarla como hormiga
      .andExpect(content().string(containsString("Farmacia")))
      .andExpect(content().string(not(containsString("/categorias/" + salud.getId() + "/"))))
      .andExpect(
        content()
          .string(
            containsString(
              "/categorias/" + kiosco.getId() + "/no-es-hormiga?volver=/mostrar-gastos"
            )
          )
      )
      .andExpect(
        content()
          .string(
            containsString(
              "Marca Kiosco como esencial: sus gastos dejan de contarse como gastos hormiga"
            )
          )
      );
  }

  private Categoria categoria(String nombre, boolean esencial, boolean fija) {
    Categoria categoria = new Categoria();
    categoria.setNombre(nombre);
    categoria.setEsencial(esencial);
    categoria.setFija(fija);
    return categoria;
  }

  private void persistirGasto(
    Double importe,
    String descripcion,
    Categoria categoria,
    Usuario usuario
  ) {
    Gasto gasto = new Gasto(importe, LocalDate.of(2026, 9, 15), descripcion);
    gasto.setUsuario(usuario);
    gasto.setCategoria(categoria);
    sessionFactory.getCurrentSession().persist(gasto);
  }

  @Test
  public void debeMandarLaAlertaDeRepeticionAlRegistrarElCuartoCafeDeLaSemana() throws Exception {
    LocalDate hoy = LocalDate.now();
    Usuario usuario = new Usuario();
    Categoria cafe = new Categoria();
    cafe.setNombre("Café y snacks");
    cafe.setEsencial(false);
    sessionFactory.getCurrentSession().persist(usuario);
    sessionFactory.getCurrentSession().persist(cafe);
    // Presupuesto del mes: 550.000, así "chico" es hasta 11.000
    sessionFactory
      .getCurrentSession()
      .persist(new Presupuesto(usuario, cafe, YearMonth.from(hoy), 550000.0));
    for (int i = 1; i <= 3; i++) {
      Gasto cafecito = new Gasto(2500.0, hoy, "Café " + i);
      cafecito.setUsuario(usuario);
      cafecito.setCategoria(cafe);
      sessionFactory.getCurrentSession().persist(cafecito);
    }

    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    MvcResult resultado = mockMvc
      .perform(
        post("/registrar-gasto")
          .sessionAttr("USUARIO", usuario)
          .param("importe", "2500")
          .param("fecha", hoy.toString())
          .param("descripcion", "Café 4")
          .param("categoria.id", cafe.getId().toString())
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(flash().attributeExists("alertaRepeticion"))
      .andReturn();

    Alerta alerta = (Alerta) resultado.getFlashMap().get("alertaRepeticion");
    assertEquals("Es tu 4.º gasto en Café y snacks esta semana ($10.000)", alerta.getMensaje());

    mockMvc
      .perform(
        get("/mostrar-gastos").sessionAttr("USUARIO", usuario).flashAttr("alertaRepeticion", alerta)
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Es tu 4.º gasto en Café y snacks esta semana")))
      .andExpect(content().string(containsString("/categorias/" + cafe.getId() + "/no-es-hormiga")))
      .andExpect(content().string(containsString("No es hormiga")));
  }

  @Test
  public void debeMostrarLasCategoriasDisponiblesEnElFormularioDeRegistro() throws Exception {
    Usuario usuario = new Usuario();
    Categoria categoria = new Categoria();
    categoria.setNombre("Alimentación");
    categoria.setDescripcion("Comidas y compras de alimentos");
    sessionFactory.getCurrentSession().persist(usuario);
    sessionFactory.getCurrentSession().persist(categoria);

    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc
      .perform(get("/registrar-gasto").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(view().name("registrar-gasto"))
      .andExpect(content().string(containsString("id=\"categoria\"")))
      .andExpect(content().string(containsString("Alimentación")));
  }

  @Test
  public void debeGuardarElGastoConLaCategoriaSeleccionadaDesdeElFormulario() throws Exception {
    Usuario usuario = new Usuario();
    Categoria categoria = new Categoria();
    categoria.setNombre("Alimentación");
    categoria.setDescripcion("Comidas y compras de alimentos");
    sessionFactory.getCurrentSession().persist(usuario);
    sessionFactory.getCurrentSession().persist(categoria);

    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    mockMvc
      .perform(
        post("/registrar-gasto")
          .sessionAttr("USUARIO", usuario)
          .param("importe", "1200")
          .param("fecha", LocalDate.now().toString())
          .param("descripcion", "Compra")
          .param("categoria.id", categoria.getId().toString())
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(view().name("redirect:/mostrar-gastos"));

    Gasto gastoGuardado = sessionFactory
      .getCurrentSession()
      .createQuery("FROM Gasto WHERE descripcion = :descripcion", Gasto.class)
      .setParameter("descripcion", "Compra")
      .getSingleResult();

    assertEquals("Alimentación", gastoGuardado.getCategoria().getNombre());
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
