package com.tallerwebi.dominio.recuperacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.RepositorioUsuario;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.TokenRecuperacionInvalido;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioRecuperacionImplTest {

  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 16, 15, 0);

  private RepositorioUsuario repositorioUsuario;
  private RepositorioTokenRecuperacion repositorioToken;
  private EnviadorDeMail enviadorDeMail;
  private ServicioRecuperacionImpl servicio;
  private Usuario lour;

  @BeforeEach
  public void init() {
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioToken = mock(RepositorioTokenRecuperacion.class);
    enviadorDeMail = mock(EnviadorDeMail.class);
    Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
    servicio =
      new ServicioRecuperacionImpl(
        repositorioUsuario,
        repositorioToken,
        enviadorDeMail,
        "http://localhost:8080/gastitos/",
        reloj
      );
    lour = new Usuario();
    lour.setEmail("lour@gastitos.com");
    lour.setPassword("vieja1");
    when(repositorioUsuario.buscar("lour@gastitos.com")).thenReturn(lour);
  }

  @Test
  public void debeMandarUnMailConElLinkYGuardarSoloElHashDelTokenQueVenceEnUnaHora() {
    servicio.solicitar("lour@gastitos.com");

    ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
    verify(enviadorDeMail)
      .enviar(eq("lour@gastitos.com"), eq(ServicioRecuperacionImpl.ASUNTO), texto.capture());
    String token = tokenDelMail(texto.getValue());
    assertThat(
      texto.getValue(),
      containsString("http://localhost:8080/gastitos/restablecer-contrasena?token=" + token)
    );

    ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
    verify(repositorioToken).guardar(guardado.capture());
    assertEquals(ServicioRecuperacionImpl.hash(token), guardado.getValue().getHash());
    assertThat(guardado.getValue().getHash(), not(containsString(token)));
    assertEquals(lour, guardado.getValue().getUsuario());
    assertEquals(AHORA.plusHours(1), guardado.getValue().getVence());
  }

  @Test
  public void noDebeMandarNadaCuandoElEmailNoTieneCuenta() {
    servicio.solicitar("nadie@gastitos.com");

    verifyNoInteractions(enviadorDeMail);
    verify(repositorioToken, never()).guardar(any());
  }

  @Test
  public void noDebeMandarNadaCuandoNoLlegaEmail() {
    servicio.solicitar(null);

    verifyNoInteractions(enviadorDeMail, repositorioToken);
  }

  @Test
  public void debeCambiarLaContrasenaYMarcarElLinkComoUsado() {
    TokenRecuperacion token = tokenGuardado("abc", AHORA.plusMinutes(30));

    servicio.restablecer("abc", "nueva123");

    assertEquals("nueva123", lour.getPassword());
    verify(repositorioUsuario).modificar(lour);
    assertTrue(token.isUsado());
    verify(repositorioToken).modificar(token);
  }

  @Test
  public void noDebeCambiarLaContrasenaConUnLinkVencido() {
    tokenGuardado("abc", AHORA.minusMinutes(1));

    assertFalse(servicio.esValido("abc"));
    assertThrows(TokenRecuperacionInvalido.class, () -> servicio.restablecer("abc", "nueva123"));
    assertEquals("vieja1", lour.getPassword());
    verify(repositorioUsuario, never()).modificar(any());
  }

  @Test
  public void noDebeCambiarLaContrasenaConUnLinkYaUsado() {
    TokenRecuperacion token = tokenGuardado("abc", AHORA.plusMinutes(30));
    token.marcarUsado();

    assertFalse(servicio.esValido("abc"));
    assertThrows(TokenRecuperacionInvalido.class, () -> servicio.restablecer("abc", "nueva123"));
    assertEquals("vieja1", lour.getPassword());
  }

  @Test
  public void noDebeCambiarLaContrasenaConUnLinkQueNoExiste() {
    when(repositorioToken.buscarPorHash(anyString())).thenReturn(null);

    assertFalse(servicio.esValido("inventado"));
    assertFalse(servicio.esValido(null));
    assertThrows(
      TokenRecuperacionInvalido.class,
      () -> servicio.restablecer("inventado", "nueva123")
    );
  }

  private TokenRecuperacion tokenGuardado(String token, LocalDateTime vence) {
    TokenRecuperacion guardado = new TokenRecuperacion(
      ServicioRecuperacionImpl.hash(token),
      lour,
      vence
    );
    when(repositorioToken.buscarPorHash(ServicioRecuperacionImpl.hash(token))).thenReturn(guardado);
    return guardado;
  }

  private String tokenDelMail(String texto) {
    int desde = texto.indexOf("token=") + "token=".length();
    return texto.substring(desde, texto.indexOf('\n', desde));
  }
}
