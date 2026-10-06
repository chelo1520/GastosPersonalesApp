package com.tallerwebi.punta_a_punta;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import com.microsoft.playwright.*;
import com.tallerwebi.punta_a_punta.vistas.VistaLogin;
import com.tallerwebi.punta_a_punta.vistas.VistaRegistrarGasto;
import com.tallerwebi.punta_a_punta.vistas.VistaSimulacion;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VistaSimulacionE2E {

  static Playwright playwright;
  static Browser browser;
  BrowserContext context;
  Page page;
  VistaSimulacion vistaSimulacion;

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
    vistaSimulacion = new VistaSimulacion(page);
  }

  @AfterEach
  void cerrarContexto() {
    context.close();
  }

  @Test
  void deberiaLlegarALaSimulacionDelMesProximoDesdeElNavbarDespuesDeIniciarSesion()
    throws MalformedURLException {
    dadoQueElUsuarioInicioSesion();
    cuandoElUsuarioTocaSimulacionEnElNavbar();
    entoncesDeberiaEstarEnLaVistaDeSimulacion();
    entoncesDeberiaVerElTituloDelMesProximo();
  }

  @Test
  void deberiaMostrarElNuevoGastoOrdinarioConElTagNuevoYSumarloAlTotal() {
    dadoQueElUsuarioInicioSesion();
    dadoQueElUsuarioEstaEnLaSimulacion();
    cuandoElUsuarioAgregaUnGastoOrdinario("Clases de guitarra", "15000");
    entoncesLaListaDeberiaTener("Clases de guitarra");
    entoncesElGastoDeberiaTenerElTag("Clases de guitarra", "Nuevo");
    entoncesElTotalSimuladoDeberiaSer("$ 15.000");
  }

  @Test
  void deberiaOcultarElAvisoDeMesVacioAlAgregarElPrimerGasto() {
    dadoQueElUsuarioInicioSesion();
    dadoQueElUsuarioEstaEnLaSimulacion();
    entoncesElAvisoDeMesVacioDeberiaEstarVisible(true);
    cuandoElUsuarioAgregaUnGastoExtraordinario("Regalo de cumpleanios", "20000");
    entoncesElAvisoDeMesVacioDeberiaEstarVisible(false);
  }

  @Test
  void deberiaRestarDelTotalElGastoExtraordinarioAlDesmarcarlo() {
    dadoQueElUsuarioInicioSesion();
    dadoQueElUsuarioEstaEnLaSimulacion();
    cuandoElUsuarioAgregaUnGastoOrdinario("Gimnasio", "10000");
    cuandoElUsuarioAgregaUnGastoExtraordinario("Regalo de cumpleanios", "20000");
    entoncesElTotalSimuladoDeberiaSer("$ 30.000");
    cuandoElUsuarioDesmarcaElGasto("Regalo de cumpleanios");
    entoncesElTotalSimuladoDeberiaSer("$ 10.000");
  }

  @Test
  void deberiaMostrarSoloLosGastosDelMesAnteriorYSumarlosAlTotal() {
    LocalDate mesAnterior = LocalDate.now().minusMonths(1);
    // El formulario solo acepta fechas del mes actual: los meses anteriores se cargan en la base
    dadoQueExisteEnLaBaseUnGasto(250000, mesAnterior.withDayOfMonth(5), "Alquiler");
    dadoQueExisteEnLaBaseUnGasto(80000, mesAnterior.withDayOfMonth(20), "Supermercado");
    dadoQueExisteEnLaBaseUnGasto(9000, mesAnterior.minusMonths(1), "Gasto de hace dos meses");
    dadoQueElUsuarioInicioSesion();
    dadoQueElUsuarioRegistroUnGasto("5000", LocalDate.now(), "Gasto del mes actual");
    cuandoElUsuarioNavegaALaSimulacion();
    entoncesLaListaDeberiaTenerEnOrden("Supermercado", "Alquiler");
    entoncesElAvisoDeMesVacioDeberiaEstarVisible(false);
    entoncesElTotalSimuladoDeberiaSer("$ 330.000");
  }

  @Test
  void deberiaRedirigirAlLoginSiSeEntraALaSimulacionSinIniciarSesion()
    throws MalformedURLException {
    cuandoElUsuarioNavegaALaSimulacion();
    entoncesDeberiaSerRedirigidoAlLogin();
  }

  private void dadoQueElUsuarioInicioSesion() {
    VistaLogin vistaLogin = new VistaLogin(page);
    vistaLogin.escribirEMAIL("test@unlam.edu.ar");
    vistaLogin.escribirClave("test");
    vistaLogin.darClickEnIniciarSesion();
    page.waitForURL("**/mostrar-gastos**");
  }

  private void dadoQueExisteEnLaBaseUnGasto(double importe, LocalDate fecha, String descripcion) {
    ReiniciarDB.insertarGastoDelUsuarioDePrueba(fecha, importe, descripcion);
  }

  private void dadoQueElUsuarioRegistroUnGasto(
    String importe,
    LocalDate fecha,
    String descripcion
  ) {
    new VistaRegistrarGasto(page).registrarGasto(importe, fecha, descripcion);
  }

  private void dadoQueElUsuarioEstaEnLaSimulacion() {
    cuandoElUsuarioNavegaALaSimulacion();
  }

  private void cuandoElUsuarioNavegaALaSimulacion() {
    page.navigate("localhost:8080/spring/simulacion");
  }

  private void cuandoElUsuarioTocaSimulacionEnElNavbar() {
    page.locator("nav.app-nav a", new Page.LocatorOptions().setHasText("Simulación")).click();
    page.waitForURL("**/simulacion**");
  }

  private void cuandoElUsuarioAgregaUnGastoOrdinario(String descripcion, String monto) {
    vistaSimulacion.agregarGastoOrdinario(descripcion, monto);
  }

  private void cuandoElUsuarioAgregaUnGastoExtraordinario(String descripcion, String monto) {
    vistaSimulacion.agregarGastoExtraordinario(descripcion, monto);
  }

  private void cuandoElUsuarioDesmarcaElGasto(String descripcion) {
    vistaSimulacion.desmarcarGasto(descripcion);
  }

  private void entoncesDeberiaEstarEnLaVistaDeSimulacion() throws MalformedURLException {
    URL url = vistaSimulacion.obtenerURLActual();
    assertThat(url.getPath(), matchesPattern("^/spring/simulacion(?:;jsessionid=[^/\\s]+)?$"));
  }

  private void entoncesDeberiaVerElTituloDelMesProximo() {
    YearMonth mesProximo = YearMonth.now().plusMonths(1);
    String nombre = mesProximo.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "AR"));
    String esperado =
      "Simulación · " +
      nombre.substring(0, 1).toUpperCase() +
      nombre.substring(1) +
      " " +
      mesProximo.getYear();
    assertThat(vistaSimulacion.obtenerTitulo().trim(), equalTo(esperado));
  }

  private void entoncesLaListaDeberiaTener(String descripcion) {
    assertThat(vistaSimulacion.obtenerDescripcionesDeLaLista(), contains(descripcion));
  }

  private void entoncesLaListaDeberiaTenerEnOrden(String... descripciones) {
    assertThat(vistaSimulacion.obtenerDescripcionesDeLaLista(), contains(descripciones));
  }

  private void entoncesElGastoDeberiaTenerElTag(String descripcion, String tag) {
    assertThat(vistaSimulacion.obtenerTagDelGasto(descripcion), equalTo(tag));
  }

  private void entoncesElTotalSimuladoDeberiaSer(String total) {
    assertThat(vistaSimulacion.obtenerTotalSimulado(), equalTo(total));
  }

  private void entoncesElAvisoDeMesVacioDeberiaEstarVisible(boolean visible) {
    assertThat(vistaSimulacion.estaVisibleElAvisoDeMesVacio(), is(visible));
  }

  private void entoncesDeberiaSerRedirigidoAlLogin() throws MalformedURLException {
    URL url = vistaSimulacion.obtenerURLActual();
    assertThat(url.getPath(), matchesPattern("^/spring/login(?:;jsessionid=[^/\\s]+)?$"));
  }
}
