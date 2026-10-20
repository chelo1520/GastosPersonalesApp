package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.recuperacion.EnviadorDeMail;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
public class RecuperacionVistaTest {

  private static final String AVISO_ENVIADO = "Si hay una cuenta con ese email";

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private SessionFactory sessionFactory;

  // El de SpringWebTestConfig: un mock, nunca manda mails de verdad
  @Autowired
  private EnviadorDeMail enviadorDeMail;

  private MockMvc mockMvc;

  @BeforeEach
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    reset(enviadorDeMail);
  }

  @Test
  public void debeMostrarElLinkOlvidasteTuContrasenaEnElLogin() throws Exception {
    mockMvc
      .perform(get("/login"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("href=\"/olvide-contrasena\"")))
      .andExpect(content().string(containsString("¿Olvidaste tu contraseña?")));
  }

  @Test
  public void debeCambiarLaContrasenaConElLinkDelMailYDejarIngresarConLaNueva() throws Exception {
    persistirUsuario("lour@gastitos.com", "vieja12");

    mockMvc
      .perform(post("/olvide-contrasena").param("email", "lour@gastitos.com"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(AVISO_ENVIADO)));

    String token = tokenDelMailEnviadoA("lour@gastitos.com");

    mockMvc
      .perform(get("/restablecer-contrasena").param("token", token))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Elegí una contraseña nueva")))
      .andExpect(content().string(containsString("id=\"password-requisitos\"")))
      .andExpect(content().string(containsString("value=\"" + token + "\"")));

    mockMvc
      .perform(
        post("/restablecer-contrasena")
          .param("token", token)
          .param("password", "Nueva123!")
          .param("confirmacion", "Nueva123!")
      )
      .andExpect(redirectedUrl("/login?restablecida"));

    mockMvc
      .perform(get("/login").param("restablecida", ""))
      .andExpect(content().string(containsString("Tu contraseña se cambió")));

    // Con la nueva entra al resumen; con la vieja ya no
    mockMvc
      .perform(
        post("/validar-login").param("email", "lour@gastitos.com").param("password", "Nueva123!")
      )
      .andExpect(redirectedUrl("/home"));
    mockMvc
      .perform(
        post("/validar-login").param("email", "lour@gastitos.com").param("password", "vieja12")
      )
      .andExpect(content().string(containsString("Usuario o clave incorrecta")));
  }

  @Test
  public void elLinkDebeServirUnaSolaVez() throws Exception {
    persistirUsuario("lour@gastitos.com", "vieja12");
    mockMvc.perform(post("/olvide-contrasena").param("email", "lour@gastitos.com"));
    String token = tokenDelMailEnviadoA("lour@gastitos.com");
    mockMvc.perform(
      post("/restablecer-contrasena")
        .param("token", token)
        .param("password", "Nueva123!")
        .param("confirmacion", "Nueva123!")
    );

    mockMvc
      .perform(get("/restablecer-contrasena").param("token", token))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("El link ya no sirve")))
      .andExpect(content().string(not(containsString("name=\"password\""))));
    mockMvc
      .perform(
        post("/restablecer-contrasena")
          .param("token", token)
          .param("password", "Otra1234!")
          .param("confirmacion", "Otra1234!")
      )
      .andExpect(content().string(containsString("El link ya no sirve")));
  }

  @Test
  public void debeMostrarElMismoAvisoSinMandarMailCuandoElEmailNoTieneCuenta() throws Exception {
    mockMvc
      .perform(post("/olvide-contrasena").param("email", "nadie@gastitos.com"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(AVISO_ENVIADO)));

    verify(enviadorDeMail, never()).enviar(anyString(), anyString(), anyString());
  }

  @Test
  public void debeAvisarQueElLinkNoSirveCuandoElTokenEsInventado() throws Exception {
    mockMvc
      .perform(get("/restablecer-contrasena").param("token", "inventado"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("El link ya no sirve")))
      .andExpect(content().string(containsString("href=\"/olvide-contrasena\"")));
  }

  private void persistirUsuario(String email, String password) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword(password);
    sessionFactory.getCurrentSession().persist(usuario);
  }

  private String tokenDelMailEnviadoA(String email) {
    ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
    verify(enviadorDeMail).enviar(eq(email), anyString(), texto.capture());
    String mail = texto.getValue();
    int desde = mail.indexOf("token=") + "token=".length();
    String token = mail.substring(desde, mail.indexOf('\n', desde));
    assertEquals(43, token.length());
    return token;
  }
}
