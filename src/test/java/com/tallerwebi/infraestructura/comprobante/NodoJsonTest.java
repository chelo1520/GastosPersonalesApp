package com.tallerwebi.infraestructura.comprobante;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class NodoJsonTest {

  @Test
  public void debeLeerSoloImportesNumericosMayoresACero() throws Exception {
    NodoJson nodo = nodo("{\"ok\":1500.5,\"cero\":0,\"negativo\":-3,\"texto\":\"1500\"}");

    assertEquals(1500.5, nodo.importe("ok"));
    assertNull(nodo.importe("cero"));
    assertNull(nodo.importe("negativo"));
    assertNull(nodo.importe("texto"));
    assertNull(nodo.importe("falta"));
  }

  @Test
  public void debeLeerTextosSinEspaciosYRechazarLosVaciosOLargos() throws Exception {
    NodoJson nodo = nodo(
      "{\"ok\":\"  Kiosco \",\"vacio\":\"   \",\"largo\":\"abcdef\",\"numero\":3}"
    );

    assertEquals("Kiosco", nodo.texto("ok", 10));
    assertNull(nodo.texto("vacio", 10));
    assertNull(nodo.texto("largo", 5));
    assertNull(nodo.texto("numero", 10));
  }

  @Test
  public void debeLeerFechasIsoYDevolverNullSiNoSeEntienden() throws Exception {
    NodoJson nodo = nodo("{\"ok\":\"2026-10-30\",\"mal\":\"30/10/2026\",\"nula\":null}");

    assertEquals(LocalDate.of(2026, 10, 30), nodo.fecha("ok"));
    assertNull(nodo.fecha("mal"));
    assertNull(nodo.fecha("nula"));
  }

  @Test
  public void debeRecorrerLosElementosDeUnaListaYQuedarVaciaSiNoEsLista() throws Exception {
    NodoJson nodo = nodo("{\"lista\":[{\"a\":\"x\"},{\"a\":\"y\"}],\"texto\":\"no\"}");

    assertEquals(2, nodo.elementos("lista").size());
    assertEquals("y", nodo.elementos("lista").get(1).texto("a", 10));
    assertTrue(nodo.elementos("texto").isEmpty());
    assertTrue(nodo.elementos("falta").isEmpty());
  }

  @Test
  public void debeLeerLosCamposDeUnHijo() throws Exception {
    NodoJson nodo = nodo("{\"confianza\":{\"importe\":\"alta\"}}");

    assertEquals("alta", nodo.hijo("confianza").texto("importe", 10));
    assertNull(nodo.hijo("falta").texto("importe", 10));
  }

  private static NodoJson nodo(String json) throws Exception {
    return new NodoJson(new ObjectMapper().readTree(json));
  }
}
