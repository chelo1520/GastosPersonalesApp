package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobante;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
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
public class ComprobanteVistaTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private LectorComprobante lectorComprobante;

  private MockMvc mockMvc;
  private Usuario usuario;
  private Categoria categoria;

  @BeforeEach
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    usuario = new Usuario();
    usuario.setEmail("comprobante@unlam.edu.ar");
    usuario.setPassword("123");
    sessionFactory.getCurrentSession().persist(usuario);

    categoria = new Categoria();
    categoria.setNombre("Alimentación");
    sessionFactory.getCurrentSession().persist(categoria);
  }

  @Test
  public void debeMostrarLasOpcionesDeCamaraYGaleriaSinAvisosDeMas() throws Exception {
    mockMvc
      .perform(get("/registrar-gasto/comprobante").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Tomar foto")))
      .andExpect(content().string(containsString("Elegir de la galería")))
      .andExpect(content().string(not(containsString("La imagen se usa solo"))));
  }

  @Test
  public void debeOfrecerLaEntradaDesdeElFormularioDeRegistro() throws Exception {
    mockMvc
      .perform(get("/registrar-gasto").sessionAttr("USUARIO", usuario))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Cargar desde comprobante")));
  }

  @Test
  public void debeMostrarLaRevisionConLosDatosPropuestosAntesDeConfirmar() throws Exception {
    byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a };
    when(lectorComprobante.leer(png, "image/png", java.util.List.of("Alimentación")))
      .thenReturn(
        new DatosComprobante(
          12500.0,
          LocalDate.of(2026, 10, 12),
          "Almacén",
          "Alimentación",
          "Transferencia enviada",
          "comprobante bancario",
          new ConfianzaComprobante(true, false, true, true)
        )
      );
    MockMultipartFile imagen = new MockMultipartFile("imagen", "ticket.png", "image/png", png);

    mockMvc
      .perform(
        multipart("/registrar-gasto/comprobante").file(imagen).sessionAttr("USUARIO", usuario)
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Revisar y confirmar datos extraídos")))
      .andExpect(content().string(containsString("value=\"12500.0\"")))
      .andExpect(content().string(containsString("Transferencia enviada")))
      .andExpect(content().string(containsString("Detectado como")))
      .andExpect(content().string(containsString("comprobante bancario")))
      .andExpect(content().string(containsString("Revisá este importe")))
      .andExpect(content().string(containsString("baja confianza")))
      .andExpect(content().string(containsString("Sacar otra foto")))
      .andExpect(content().string(containsString("Confirmar y guardar")));
  }
}
