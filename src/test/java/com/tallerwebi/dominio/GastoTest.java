package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tallerwebi.dominio.gasto.Gasto;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class GastoTest {

  @Test
  void DeberiaCrearGastoConSusAtributos() {
    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado");

    assertEquals(1000.00, gasto.getImporte());
    assertEquals(LocalDate.of(2026, 9, 13), gasto.getFecha());
    assertEquals("Supermercado", gasto.getDescripcion());
  }
}
