package com.tallerwebi.punta_a_punta;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

import com.microsoft.playwright.*;
import com.tallerwebi.punta_a_punta.vistas.VistaLogin;
import com.tallerwebi.punta_a_punta.vistas.VistaPerfil;
import com.tallerwebi.punta_a_punta.vistas.VistaWeb;
import java.net.URI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VistaPerfilE2E {

  static Playwright playwright;
  static Browser browser;
  BrowserContext context;
  Page page;
  VistaPerfil vistaPerfil;

  @BeforeAll
  static void abrirNavegador() {
    playwright = Playwright.create();
    boolean verNavegador = Boolean.parseBoolean(System.getProperty("verNavegador", "false"));
    browser =
      playwright
        .chromium()
        .launch(
          new BrowserType.LaunchOptions()
            .setHeadless(!verNavegador)
            .setSlowMo(verNavegador ? 2000 : 0)
        );
  }

  @AfterAll
  static void cerrarNavegador() {
    playwright.close();
  }

  @BeforeEach
  void crearContextoYPagina() {
    ReiniciarDB.limpiarBaseDeDatos();
    context = browser.newContext();
    page = context.newPage();
  }

  @AfterEach
  void cerrarContexto() {
    context.close();
  }

  @Test
  void deberiaNavegarAlPerfilDesdeElNavbarYMostrarElEmail() {
    dadoQueElUsuarioInicioSesion();
    cuandoNavegaAlPerfilDesdeElNavbar();

    assertThat(URI.create(page.url()).getPath(), matchesPattern(VistaWeb.patronDeRuta("/perfil")));
    assertThat(vistaPerfil.obtenerEmail(), equalTo("test@unlam.edu.ar"));
  }

  @Test
  void deberiaRedirigirAlLoginSiSeEntraAlPerfilSinIniciarSesion() {
    page.navigate(VistaWeb.URL_BASE + "/perfil");

    assertThat(URI.create(page.url()).getPath(), matchesPattern(VistaWeb.patronDeRuta("/login")));
  }

  private void dadoQueElUsuarioInicioSesion() {
    VistaLogin vistaLogin = new VistaLogin(page);
    vistaLogin.escribirEMAIL("test@unlam.edu.ar");
    vistaLogin.escribirClave("test");
    vistaLogin.darClickEnIniciarSesion();
    page.waitForURL("**/mostrar-gastos**");
  }

  private void cuandoNavegaAlPerfilDesdeElNavbar() {
    page.locator("nav.app-nav a", new Page.LocatorOptions().setHasText("Perfil")).click();
    page.waitForURL("**/perfil**");
    vistaPerfil = new VistaPerfil(page);
  }
}
