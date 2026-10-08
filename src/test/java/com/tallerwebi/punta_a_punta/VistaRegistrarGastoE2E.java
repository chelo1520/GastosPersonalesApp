package com.tallerwebi.punta_a_punta;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import com.microsoft.playwright.*;
import com.tallerwebi.punta_a_punta.vistas.VistaLogin;
import com.tallerwebi.punta_a_punta.vistas.VistaMisGastos;
import com.tallerwebi.punta_a_punta.vistas.VistaRegistrarGasto;
import com.tallerwebi.punta_a_punta.vistas.VistaWeb;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VistaRegistrarGastoE2E {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  static Playwright playwright;
  static Browser browser;
  BrowserContext context;
  Page page;
  VistaRegistrarGasto vistaRegistrarGasto;

  @BeforeAll
  static void abrirNavegador() {
    playwright = Playwright.create();
    // Con -DverNavegador=true se abre la ventana y se ve cada paso en cámara lenta
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
  void deberiaVolverAMisGastosYMostrarElGastoConSuFechaImporteYTotal()
    throws MalformedURLException {
    dadoQueElUsuarioInicioSesion();
    dadoQueElUsuarioEstaEnRegistrarGasto();
    LocalDate hoy = LocalDate.now();
    cuandoElUsuarioRegistraUnGasto("46200", hoy, "Compra semanal");
    entoncesDeberiaEstarEnMisGastos();
    entoncesLaTablaDeberiaTenerLaFila(hoy.format(FORMATO_FECHA) + " Compra semanal $ 46.200,00");
    entoncesElTotalDeberiaSer("$ 46.200,00");
  }

  @Test
  void deberiaQuedarseEnRegistrarGastoSinGuardarSiElImporteEsCero() throws MalformedURLException {
    dadoQueElUsuarioInicioSesion();
    dadoQueElUsuarioEstaEnRegistrarGasto();
    cuandoElUsuarioCompletaElFormularioYTocaGuardar("0", LocalDate.now(), "Gasto cero");
    entoncesElImporteDeberiaSerInvalido();
    entoncesDeberiaSeguirEnRegistrarGasto();
  }

  @Test
  void deberiaQuedarseEnRegistrarGastoSinGuardarSiLaFechaEsDelMesAnterior()
    throws MalformedURLException {
    LocalDate mesAnterior = LocalDate.now().minusMonths(1);
    dadoQueElUsuarioInicioSesion();
    dadoQueElUsuarioEstaEnRegistrarGasto();
    cuandoElUsuarioCompletaElFormularioYTocaGuardar("1000", mesAnterior, "Gasto del mes pasado");
    entoncesLaFechaDeberiaSerInvalida();
    entoncesDeberiaSeguirEnRegistrarGasto();
  }

  @Test
  void deberiaRedirigirAlLoginSiSeEntraARegistrarGastoSinIniciarSesion()
    throws MalformedURLException {
    dadoQueElUsuarioEstaEnRegistrarGasto();
    entoncesDeberiaSerRedirigidoAlLogin();
  }

  private void dadoQueElUsuarioInicioSesion() {
    VistaLogin vistaLogin = new VistaLogin(page);
    vistaLogin.escribirEMAIL("test@unlam.edu.ar");
    vistaLogin.escribirClave("test");
    vistaLogin.darClickEnIniciarSesion();
    page.waitForURL("**/mostrar-gastos**");
  }

  private void dadoQueElUsuarioEstaEnRegistrarGasto() {
    vistaRegistrarGasto = new VistaRegistrarGasto(page);
  }

  private void cuandoElUsuarioRegistraUnGasto(String importe, LocalDate fecha, String descripcion) {
    vistaRegistrarGasto.registrarGasto(importe, fecha, descripcion);
  }

  private void cuandoElUsuarioCompletaElFormularioYTocaGuardar(
    String importe,
    LocalDate fecha,
    String descripcion
  ) {
    vistaRegistrarGasto.completarFormulario(importe, fecha, descripcion);
    vistaRegistrarGasto.darClickEnGuardar();
  }

  private void entoncesDeberiaEstarEnMisGastos() throws MalformedURLException {
    assertThat(
      vistaRegistrarGasto.obtenerURLActual().getPath(),
      matchesPattern(VistaWeb.patronDeRuta("/mostrar-gastos"))
    );
  }

  private void entoncesLaTablaDeberiaTenerLaFila(String fila) {
    assertThat(new VistaMisGastos(page).obtenerFilasDeLaTabla(), contains(fila));
  }

  private void entoncesElTotalDeberiaSer(String total) {
    assertThat(new VistaMisGastos(page).obtenerTotal(), equalTo(total));
  }

  private void entoncesElImporteDeberiaSerInvalido() {
    assertThat(vistaRegistrarGasto.elImporteEsValido(), is(false));
  }

  private void entoncesLaFechaDeberiaSerInvalida() {
    assertThat(vistaRegistrarGasto.laFechaEsValida(), is(false));
  }

  private void entoncesDeberiaSeguirEnRegistrarGasto() throws MalformedURLException {
    URL url = vistaRegistrarGasto.obtenerURLActual();
    assertThat(url.getPath(), matchesPattern(VistaWeb.patronDeRuta("/registrar-gasto")));
  }

  private void entoncesDeberiaSerRedirigidoAlLogin() throws MalformedURLException {
    URL url = vistaRegistrarGasto.obtenerURLActual();
    assertThat(url.getPath(), matchesPattern(VistaWeb.patronDeRuta("/login")));
  }
}
