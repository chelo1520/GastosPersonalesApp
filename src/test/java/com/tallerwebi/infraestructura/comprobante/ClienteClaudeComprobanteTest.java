package com.tallerwebi.infraestructura.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import java.net.http.HttpClient;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class ClienteClaudeComprobanteTest {

  @Test
  public void debeExtraerLosCamposDelJsonDevueltoPorClaude() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      mock(HttpClient.class)
    );
    String respuesta =
      """
      {"content":[{"type":"text","text":"{\\"tipoOperacion\\":\\"Transferencia enviada\\",\\"tipoComprobante\\":\\"comprobante bancario\\",\\"importe\\":12500.5,\\"fecha\\":\\"2026-10-12\\",\\"descripcion\\":\\"Almacén\\",\\"categoria\\":\\"Alimentación\\",\\"confianza\\":{\\"importe\\":\\"alta\\",\\"fecha\\":\\"media\\",\\"descripcion\\":\\"baja\\",\\"categoria\\":\\"alta\\"}}"}]}
      """;

    DatosComprobante datos = cliente.extraerDatos(respuesta);

    assertEquals(12500.5, datos.importe());
    assertEquals(LocalDate.of(2026, 10, 12), datos.fecha());
    assertEquals("Almacén", datos.descripcion());
    assertEquals("Alimentación", datos.categoria());
    assertEquals("Transferencia enviada", datos.tipoOperacion());
    assertEquals("comprobante bancario", datos.tipoComprobante());
    assertEquals(new ConfianzaComprobante(false, false, true, false), datos.confianza());
  }

  @Test
  public void debeRechazarUnaRespuestaSinLosDatosObligatorios() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      mock(HttpClient.class)
    );
    String respuesta =
      """
      {"content":[{"type":"text","text":"{\\"importe\\":null,\\"fecha\\":null,\\"descripcion\\":null,\\"tipoOperacion\\":\\"Ticket\\",\\"tipoComprobante\\":\\"ticket\\",\\"confianza\\":{}}"}]}
      """;

    assertThrows(ComprobanteNoLegible.class, () -> cliente.extraerDatos(respuesta));
  }

  @Test
  public void debeRechazarUnaRespuestaQueNoSeaJson() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      mock(HttpClient.class)
    );

    assertThrows(
      ComprobanteNoLegible.class,
      () -> cliente.extraerDatos("{\"content\":[{\"type\":\"text\",\"text\":\"No se leyó\"}]}")
    );
  }

  @Test
  public void debeRechazarUnaRespuestaCortadaPorFaltaDeTokens() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      mock(HttpClient.class)
    );
    String respuesta =
      """
      {"stop_reason":"max_tokens","content":[{"type":"text","text":"{\"tipoOperacion\":\"Compra\",\"importe\":13350,\"fecha\":\"2026-10-12\",\"descripcion\":\"Super\",\"tipoComprobante\":\"ticket\",\"confianza\":{\"importe\":"}]}
      """;

    assertThrows(ComprobanteNoLegible.class, () -> cliente.extraerDatos(respuesta));
  }

  @Test
  public void debePedirSinRazonamientoYConMargenDeTokensParaQueElJsonNoSeCorte() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      mock(HttpClient.class)
    );

    ObjectNode cuerpo = cliente.crearCuerpo(
      new byte[] { 1 },
      "image/jpeg",
      java.util.List.of("Alimentación")
    );

    assertEquals("disabled", cuerpo.path("thinking").path("type").asText());
    assertTrue(cuerpo.path("max_tokens").asInt() >= 1024);
    assertEquals("image", primerBloque(cuerpo).path("type").asText());
    assertEquals("image/jpeg", primerBloque(cuerpo).path("source").path("media_type").asText());
  }

  @Test
  public void debeMandarUnPdfComoDocumentoYNoComoImagen() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      mock(HttpClient.class)
    );

    ObjectNode cuerpo = cliente.crearCuerpo(
      new byte[] { 0x25, 0x50, 0x44, 0x46 },
      "application/pdf",
      java.util.List.of()
    );

    assertEquals("document", primerBloque(cuerpo).path("type").asText());
    assertEquals(
      "application/pdf",
      primerBloque(cuerpo).path("source").path("media_type").asText()
    );
  }

  private static JsonNode primerBloque(ObjectNode cuerpo) {
    return cuerpo.path("messages").get(0).path("content").get(0);
  }

  @Test
  public void noDebeLlamarAClaudeCuandoNoHayClaveConfigurada() {
    HttpClient http = mock(HttpClient.class);
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante("", http);

    assertThrows(
      com.tallerwebi.dominio.comprobante.LectorComprobanteNoDisponible.class,
      () -> cliente.leer(new byte[] { 1 }, "image/png", java.util.List.of())
    );
    verifyNoInteractions(http);
  }
}
