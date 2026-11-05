package com.tallerwebi.infraestructura.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.comprobante.ComprobanteNoLegible;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Lectura del JSON que escribe el modelo; no hay llamadas a la API. */
public class ParserComprobanteTest {

  private final ParserComprobante parser = new ParserComprobante();

  @Test
  public void debeLeerUnComprobanteDeUnSoloGasto() {
    DatosComprobante datos = parser.parsear(
      """
      {"tipoOperacion":"Transferencia enviada","tipoComprobante":"comprobante bancario",
       "total":12500.5,"gastos":[
         {"importe":12500.5,"fecha":"2026-10-12","descripcion":"Almacén","detalle":null,
          "categoria":"Alimentación",
          "confianza":{"importe":"alta","fecha":"media","descripcion":"baja","categoria":"alta"}}]}
      """
    );

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
    DatosComprobante datos = parser.parsear(
      """
      {"tipoOperacion":"Compra","tipoComprobante":"ticket de supermercado","total":9800,"gastos":[
        {"importe":6300,"fecha":"2026-10-30","descripcion":"Supermercado Día","detalle":"Leche, pan",
         "categoria":"Alimentación","confianza":{"importe":"alta","fecha":"alta","descripcion":"alta","categoria":"alta"}},
        {"importe":3500,"fecha":"2026-10-30","descripcion":"Supermercado Día","detalle":"Caramelos, alfajores",
         "categoria":"Café y snacks","confianza":{"importe":"alta","fecha":"alta","descripcion":"alta","categoria":"media"}}]}
      """
    );

    assertEquals(2, datos.gastos().size());
    assertEquals("Leche, pan", datos.gastos().get(0).detalle());
    assertEquals("Café y snacks", datos.gastos().get(1).categoria());
    assertEquals(9800.0, datos.total());
  }

  @Test
  public void debeDescartarLosRenglonesSinImporteOSinComercio() {
    DatosComprobante datos = parser.parsear(
      """
      {"tipoOperacion":"Consumos","tipoComprobante":"resumen de tarjeta","total":null,"gastos":[
        {"importe":null,"fecha":"2026-10-02","descripcion":"Ilegible","confianza":{}},
        {"importe":5000,"fecha":"2026-10-02","descripcion":"  ","confianza":{}},
        {"importe":8000,"fecha":"2026-10-03","descripcion":"Peluquería Sol","confianza":{}}]}
      """
    );

    assertNull(datos.total());
    assertEquals(1, datos.gastos().size());
    assertEquals("Peluquería Sol", datos.gastos().get(0).descripcion());
  }

  @Test
  public void debeRechazarUnComprobanteSinNingunGastoLegible() {
    assertThrows(
      ComprobanteNoLegible.class,
      () ->
        parser.parsear(
          """
          {"tipoOperacion":"Ticket","tipoComprobante":"ticket","total":null,"gastos":[
            {"importe":null,"fecha":null,"descripcion":null,"confianza":{}}]}
          """
        )
    );
  }

  @Test
  public void debeRechazarUnComprobanteSinTipoDeOperacion() {
    assertThrows(
      ComprobanteNoLegible.class,
      () ->
        parser.parsear(
          "{\"tipoComprobante\":\"ticket\",\"gastos\":[{\"importe\":1,\"descripcion\":\"Kiosco\"}]}"
        )
    );
  }

  @Test
  public void debeRechazarUnTextoQueNoSeaJson() {
    assertThrows(ComprobanteNoLegible.class, () -> parser.parsear("No se leyó"));
  }

  @Test
  public void noDebeProponerMasDelMaximoDeGastos() {
    StringBuilder gastos = new StringBuilder();
    for (int i = 0; i < ParserComprobante.MAXIMO_DE_GASTOS + 5; i++) {
      gastos.append(i == 0 ? "" : ",").append("{\"importe\":100,\"descripcion\":\"Consumo\"}");
    }

    DatosComprobante datos = parser.parsear(
      "{\"tipoOperacion\":\"Consumos\",\"tipoComprobante\":\"resumen\",\"gastos\":[" + gastos + "]}"
    );

    assertEquals(ParserComprobante.MAXIMO_DE_GASTOS, datos.gastos().size());
  }

  @Test
  public void debeMarcarParaRevisarLaFechaQueFaltaAunqueElModeloDigaQueEsConfiable()
    throws Exception {
    GastoDelComprobante gasto = parser.parsearGasto(
      nodo(
        "{\"importe\":8000,\"fecha\":null,\"descripcion\":\"Cine\",\"confianza\":{\"fecha\":\"alta\"}}"
      )
    );

    assertNull(gasto.fecha());
    assertTrue(gasto.confianza().fechaBajaConfianza());
  }

  @Test
  public void debeConsiderarBajaCualquierConfianzaQueNoSeaAltaNiMedia() throws Exception {
    ConfianzaComprobante confianza = parser.confianza(
      nodo("{\"importe\":\"ALTA\",\"fecha\":\"Media\",\"descripcion\":\"dudosa\"}"),
      false
    );

    assertEquals(new ConfianzaComprobante(false, false, true, true), confianza);
  }

  private static NodoJson nodo(String json) throws Exception {
    return new NodoJson(new ObjectMapper().readTree(json));
  }
}
