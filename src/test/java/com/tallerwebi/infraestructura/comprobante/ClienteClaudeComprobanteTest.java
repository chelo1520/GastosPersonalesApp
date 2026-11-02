package com.tallerwebi.infraestructura.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import java.net.http.HttpClient;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class ClienteClaudeComprobanteTest {

  private static final String URL_MENSAJES = "http://claude.test/v1/messages";

  @Test
  public void debeExtraerLosCamposDelJsonDevueltoPorClaude() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      URL_MENSAJES,
      mock(HttpClient.class)
    );
    String respuesta = respuestaDeClaude(
      """
      {"tipoOperacion":"Transferencia enviada","tipoComprobante":"comprobante bancario",
       "total":12500.5,"gastos":[
         {"importe":12500.5,"fecha":"2026-10-12","descripcion":"Almacén","detalle":null,
          "categoria":"Alimentación",
          "confianza":{"importe":"alta","fecha":"media","descripcion":"baja","categoria":"alta"}}]}
      """
    );

    DatosComprobante datos = cliente.extraerDatos(respuesta);

    assertEquals("Transferencia enviada", datos.tipoOperacion());
    assertEquals("comprobante bancario", datos.tipoComprobante());
    assertEquals(12500.5, datos.total());
    assertEquals(1, datos.gastos().size());
    GastoDelComprobante gasto = datos.gastos().get(0);
    assertEquals(12500.5, gasto.importe());
    assertEquals(LocalDate.of(2026, 10, 12), gasto.fecha());
    assertEquals("Almacén", gasto.descripcion());
    assertNull(gasto.detalle());
    assertEquals("Alimentación", gasto.categoria());
    assertEquals(new ConfianzaComprobante(false, false, true, false), gasto.confianza());
  }

  @Test
  public void debeSepararUnTicketEnUnGastoPorCategoria() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      URL_MENSAJES,
      mock(HttpClient.class)
    );
    String respuesta = respuestaDeClaude(
      """
      {"tipoOperacion":"Compra","tipoComprobante":"ticket de supermercado","total":9800,"gastos":[
        {"importe":6300,"fecha":"2026-10-30","descripcion":"Supermercado Día","detalle":"Leche, pan",
         "categoria":"Alimentación","confianza":{"importe":"alta","fecha":"alta","descripcion":"alta","categoria":"alta"}},
        {"importe":3500,"fecha":"2026-10-30","descripcion":"Supermercado Día","detalle":"Caramelos, alfajores",
         "categoria":"Café y snacks","confianza":{"importe":"alta","fecha":"alta","descripcion":"alta","categoria":"media"}}]}
      """
    );

    DatosComprobante datos = cliente.extraerDatos(respuesta);

    assertEquals(2, datos.gastos().size());
    assertEquals("Leche, pan", datos.gastos().get(0).detalle());
    assertEquals("Café y snacks", datos.gastos().get(1).categoria());
    assertEquals(9800.0, datos.sumaDeLosGastos());
  }

  @Test
  public void debeDescartarLosRenglonesIlegiblesYPedirRevisarLaFechaQueFalta() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      URL_MENSAJES,
      mock(HttpClient.class)
    );
    String respuesta = respuestaDeClaude(
      """
      {"tipoOperacion":"Consumos","tipoComprobante":"resumen de tarjeta","total":null,"gastos":[
        {"importe":null,"fecha":"2026-10-02","descripcion":"Ilegible","confianza":{}},
        {"importe":8000,"fecha":null,"descripcion":"Peluquería Sol","categoria":"Estética y cuidado personal",
         "confianza":{"importe":"alta","fecha":"alta","descripcion":"alta","categoria":"alta"}}]}
      """
    );

    DatosComprobante datos = cliente.extraerDatos(respuesta);

    assertNull(datos.total());
    assertEquals(1, datos.gastos().size());
    assertEquals("Peluquería Sol", datos.gastos().get(0).descripcion());
    assertNull(datos.gastos().get(0).fecha());
    assertTrue(datos.gastos().get(0).confianza().fechaBajaConfianza());
  }

  @Test
  public void debeRechazarUnaRespuestaSinNingunGastoLegible() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      URL_MENSAJES,
      mock(HttpClient.class)
    );
    String respuesta = respuestaDeClaude(
      """
      {"tipoOperacion":"Ticket","tipoComprobante":"ticket","total":null,"gastos":[
        {"importe":null,"fecha":null,"descripcion":null,"confianza":{}}]}
      """
    );

    assertThrows(ComprobanteNoLegible.class, () -> cliente.extraerDatos(respuesta));
  }

  // La respuesta de la API trae el JSON pedido como texto dentro de un bloque "text"
  private static String respuestaDeClaude(String jsonPedido) {
    ObjectMapper json = new ObjectMapper();
    ObjectNode raiz = json.createObjectNode();
    ObjectNode bloque = raiz.putArray("content").addObject();
    bloque.put("type", "text");
    bloque.put("text", jsonPedido);
    return raiz.toString();
  }

  @Test
  public void debeRechazarUnaRespuestaQueNoSeaJson() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      URL_MENSAJES,
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
      URL_MENSAJES,
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
      URL_MENSAJES,
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
      URL_MENSAJES,
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
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante("", URL_MENSAJES, http);

    assertThrows(
      com.tallerwebi.dominio.comprobante.LectorComprobanteNoDisponible.class,
      () -> cliente.leer(new byte[] { 1 }, "image/png", java.util.List.of())
    );
    verifyNoInteractions(http);
  }
}
