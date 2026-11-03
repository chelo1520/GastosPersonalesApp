package com.tallerwebi.infraestructura.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobante;
import com.tallerwebi.dominio.comprobante.LectorComprobanteNoDisponible;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Armado del pedido y lectura de la respuesta de la API; el HttpClient y el parser son mocks. */
public class ClienteClaudeComprobanteTest {

  private static final String URL_MENSAJES = "http://claude.test/v1/messages";
  private static final byte[] ARCHIVO = new byte[] { 1 };

  private HttpClient http;
  private ParserComprobante parser;
  private ClienteClaudeComprobante cliente;

  @BeforeEach
  public void init() {
    http = mock(HttpClient.class);
    parser = mock(ParserComprobante.class);
    cliente = new ClienteClaudeComprobante("clave", URL_MENSAJES, parser, http);
  }

  @Test
  public void debeDevolverLoQueLeeElParserDelJsonQueEscribioElModelo() throws Exception {
    DatosComprobante datos = new DatosComprobante(
      "Compra",
      "ticket",
      null,
      List.of(
        new GastoDelComprobante(
          100.0,
          null,
          "Kiosco",
          null,
          null,
          new ConfianzaComprobante(false, true, false, true)
        )
      )
    );
    respondeLaApi(200, sobre("{\"gastos\":[]}"));
    when(parser.parsear("{\"gastos\":[]}")).thenReturn(datos);
    LectorComprobante lector = cliente;

    assertSame(datos, lector.leer(ARCHIVO, "image/png", List.of()));
  }

  @Test
  public void noDebeLlamarAClaudeCuandoNoHayClaveConfigurada() {
    LectorComprobante sinClave = new ClienteClaudeComprobante("", URL_MENSAJES, parser, http);

    assertThrows(
      LectorComprobanteNoDisponible.class,
      () -> sinClave.leer(ARCHIVO, "image/png", List.of())
    );
    verifyNoInteractions(http, parser);
  }

  @Test
  public void debeAvisarQueNoEstaDisponibleCuandoLaApiNoRespondeOk() throws Exception {
    respondeLaApi(529, "{\"error\":\"overloaded\"}");

    assertThrows(
      LectorComprobanteNoDisponible.class,
      () -> cliente.leer(ARCHIVO, "image/png", List.of())
    );
    verifyNoInteractions(parser);
  }

  @Test
  public void debeRechazarUnaRespuestaSinBloqueDeTexto() {
    assertThrows(ComprobanteNoLegible.class, () -> cliente.textoDelModelo("{\"content\":[]}"));
  }

  @Test
  public void debeRechazarUnaRespuestaQueNoSeaJson() {
    assertThrows(ComprobanteNoLegible.class, () -> cliente.textoDelModelo("<html>error</html>"));
  }

  @Test
  public void debeRechazarUnaRespuestaCortadaPorFaltaDeTokens() {
    String respuesta =
      "{\"stop_reason\":\"max_tokens\",\"content\":[{\"type\":\"text\",\"text\":\"{\\\"gastos\\\":[\"}]}";

    assertThrows(ComprobanteNoLegible.class, () -> cliente.textoDelModelo(respuesta));
  }

  @Test
  public void debePedirSinRazonamientoYConMargenDeTokensParaQueElJsonNoSeCorte() {
    ObjectNode cuerpo = cliente.crearCuerpo(ARCHIVO, "image/jpeg", List.of("Alimentación"));

    assertEquals("disabled", cuerpo.path("thinking").path("type").asText());
    assertTrue(cuerpo.path("max_tokens").asInt() >= 1024);
    assertEquals("image", primerBloque(cuerpo).path("type").asText());
    assertEquals("image/jpeg", primerBloque(cuerpo).path("source").path("media_type").asText());
  }

  @Test
  public void debeMandarUnPdfComoDocumentoYNoComoImagen() {
    ObjectNode cuerpo = cliente.crearCuerpo(
      new byte[] { 0x25, 0x50, 0x44, 0x46 },
      "application/pdf",
      List.of()
    );

    assertEquals("document", primerBloque(cuerpo).path("type").asText());
    assertEquals(
      "application/pdf",
      primerBloque(cuerpo).path("source").path("media_type").asText()
    );
  }

  @Test
  public void debePedirLaSeparacionPorCategoriaYPorConsumoEnElPrompt() {
    ObjectNode cuerpo = cliente.crearCuerpo(ARCHIVO, "image/png", List.of("Alimentación"));
    String prompt = cuerpo.path("messages").get(0).path("content").get(1).path("text").asText();

    assertTrue(prompt.contains("un gasto por categoría"));
    assertTrue(prompt.contains("un gasto por cada consumo"));
    assertTrue(prompt.contains("[\"Alimentación\"]"));
  }

  private void respondeLaApi(int estado, String cuerpo) throws Exception {
    @SuppressWarnings("unchecked")
    HttpResponse<String> respuesta = mock(HttpResponse.class);
    when(respuesta.statusCode()).thenReturn(estado);
    when(respuesta.body()).thenReturn(cuerpo);
    doReturn(respuesta).when(http).send(any(), any());
  }

  // La API devuelve el JSON pedido como texto dentro de un bloque "text"
  private static String sobre(String jsonDelModelo) {
    ObjectNode raiz = new ObjectMapper().createObjectNode();
    ObjectNode bloque = raiz.putArray("content").addObject();
    bloque.put("type", "text");
    bloque.put("text", jsonDelModelo);
    return raiz.toString();
  }

  private static JsonNode primerBloque(ObjectNode cuerpo) {
    return cuerpo.path("messages").get(0).path("content").get(0);
  }
}
