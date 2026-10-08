package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class VistaWeb {

  public static final String CONTEXT_PATH = "/gastitos";
  public static final String URL_BASE = "http://localhost:" + puertoDeLaApp() + CONTEXT_PATH;

  protected Page page;

  public VistaWeb(Page page) {
    this.page = page;
  }

  // Mismo nombre de variable que usa docker-compose para publicar la app
  private static String puertoDeLaApp() {
    String puerto = System.getenv("APP_HOST_PORT");
    return puerto != null ? puerto : "8080";
  }

  // Patron para comparar el path actual, tolerando el ;jsessionid que agrega el servidor
  public static String patronDeRuta(String ruta) {
    return "^" + CONTEXT_PATH + ruta + "(?:;jsessionid=[^/\\s]+)?$";
  }

  public URL obtenerURLActual() throws MalformedURLException {
    URL url = URI.create(page.url()).toURL();
    return url;
  }

  protected String obtenerTextoDelElemento(String selectorCSS) {
    return this.obtenerElemento(selectorCSS).textContent();
  }

  protected void darClickEnElElemento(String selectorCSS) {
    this.obtenerElemento(selectorCSS).click();
  }

  protected void escribirEnElElemento(String selectorCSS, String texto) {
    this.obtenerElemento(selectorCSS).type(texto);
  }

  private Locator obtenerElemento(String selectorCSS) {
    return page.locator(selectorCSS);
  }
}
