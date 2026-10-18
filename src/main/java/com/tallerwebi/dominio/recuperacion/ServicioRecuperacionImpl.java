package com.tallerwebi.dominio.recuperacion;

import com.tallerwebi.dominio.RepositorioUsuario;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.TokenRecuperacionInvalido;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Link de un solo uso, que vence en una hora, para elegir una contraseña nueva. */
@Service
@Transactional
public class ServicioRecuperacionImpl implements ServicioRecuperacion {

  static final int HORAS_DE_VALIDEZ = 1;
  static final String ASUNTO = "Cambiá tu contraseña de Gastitos";
  private static final int BYTES_DEL_TOKEN = 32;

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioTokenRecuperacion repositorioToken;
  private final EnviadorDeMail enviadorDeMail;
  // Dirección pública de la app: el link del mail no se arma con el Host del pedido, que lo
  // puede elegir quien hace el pedido y mandar el link a su propio sitio
  private final String urlDeLaApp;
  private final Clock reloj;
  private final SecureRandom azar = new SecureRandom();

  @Autowired
  public ServicioRecuperacionImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioTokenRecuperacion repositorioToken,
    EnviadorDeMail enviadorDeMail,
    @Value("${APP_URL:http://localhost:8080/gastitos}") String urlDeLaApp
  ) {
    this(
      repositorioUsuario,
      repositorioToken,
      enviadorDeMail,
      urlDeLaApp,
      Clock.systemDefaultZone()
    );
  }

  ServicioRecuperacionImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioTokenRecuperacion repositorioToken,
    EnviadorDeMail enviadorDeMail,
    String urlDeLaApp,
    Clock reloj
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioToken = repositorioToken;
    this.enviadorDeMail = enviadorDeMail;
    this.urlDeLaApp =
      urlDeLaApp.endsWith("/") ? urlDeLaApp.substring(0, urlDeLaApp.length() - 1) : urlDeLaApp;
    this.reloj = reloj;
  }

  @Override
  public void solicitar(String email) {
    Usuario usuario = email == null ? null : repositorioUsuario.buscar(email.trim());
    if (usuario == null) {
      return;
    }

    String token = nuevoToken();
    repositorioToken.guardar(
      new TokenRecuperacion(hash(token), usuario, ahora().plusHours(HORAS_DE_VALIDEZ))
    );
    enviadorDeMail.enviar(usuario.getEmail(), ASUNTO, textoDelMail(token));
  }

  @Override
  public boolean esValido(String token) {
    return buscarValido(token) != null;
  }

  @Override
  public void restablecer(String token, String passwordNueva) {
    TokenRecuperacion recuperacion = buscarValido(token);
    if (recuperacion == null) {
      throw new TokenRecuperacionInvalido();
    }
    Usuario usuario = recuperacion.getUsuario();
    usuario.setPassword(passwordNueva);
    repositorioUsuario.modificar(usuario);
    recuperacion.marcarUsado();
    repositorioToken.modificar(recuperacion);
  }

  private TokenRecuperacion buscarValido(String token) {
    if (token == null || token.isBlank()) {
      return null;
    }
    TokenRecuperacion recuperacion = repositorioToken.buscarPorHash(hash(token));
    return recuperacion != null && recuperacion.esValido(ahora()) ? recuperacion : null;
  }

  private String textoDelMail(String token) {
    return (
      "Hola,\n\n" +
      "Pediste cambiar tu contraseña de Gastitos. Entrá a este link para elegir una nueva:\n\n" +
      urlDeLaApp +
      "/restablecer-contrasena?token=" +
      token +
      "\n\n" +
      "El link vence en " +
      HORAS_DE_VALIDEZ +
      " hora y sirve una sola vez.\n" +
      "Si no lo pediste vos, ignorá este mail: tu contraseña no cambia.\n"
    );
  }

  private String nuevoToken() {
    byte[] bytes = new byte[BYTES_DEL_TOKEN];
    azar.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String hash(String token) {
    try {
      byte[] digest = MessageDigest
        .getInstance("SHA-256")
        .digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      // Toda JVM trae SHA-256
      throw new IllegalStateException(e);
    }
  }

  private LocalDateTime ahora() {
    return LocalDateTime.now(reloj);
  }
}
