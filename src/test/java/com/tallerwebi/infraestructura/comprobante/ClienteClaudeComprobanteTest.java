package com.tallerwebi.infraestructura.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
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
      {"content":[{"type":"text","text":"{\\"importe\\":12500.5,\\"fecha\\":\\"2026-10-12\\",\\"descripcion\\":\\"Almacén\\",\\"categoria\\":\\"Alimentación\\"}"}]}
      """;

    DatosComprobante datos = cliente.extraerDatos(respuesta);

    assertEquals(12500.5, datos.importe());
    assertEquals(LocalDate.of(2026, 10, 12), datos.fecha());
    assertEquals("Almacén", datos.descripcion());
    assertEquals("Alimentación", datos.categoria());
  }

  @Test
  public void debeRechazarUnaRespuestaSinLosDatosObligatorios() {
    ClienteClaudeComprobante cliente = new ClienteClaudeComprobante(
      "clave",
      mock(HttpClient.class)
    );
    String respuesta =
      """
      {"content":[{"type":"text","text":"{\\"importe\\":null,\\"fecha\\":null,\\"descripcion\\":null}"}]}
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
