package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;

public class OrigenDelRegistroTest {

  private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);

  @Test
  public void soloReconoceElResumenYMisGastos() {
    assertEquals(OrigenDelRegistro.RESUMEN, OrigenDelRegistro.de("resumen"));
    assertEquals(OrigenDelRegistro.MIS_GASTOS, OrigenDelRegistro.de("mis-gastos"));
    assertNull(OrigenDelRegistro.de("https://otro-sitio.com"));
    assertNull(OrigenDelRegistro.de(null));
  }

  @Test
  public void elMesActualVaSinParametroYOtroMesLoLleva() {
    assertEquals("redirect:/home", OrigenDelRegistro.RESUMEN.redireccion(NOVIEMBRE, NOVIEMBRE));
    assertEquals(
      "redirect:/mostrar-gastos?mes=2026-10",
      OrigenDelRegistro.MIS_GASTOS.redireccion(YearMonth.of(2026, 10), NOVIEMBRE)
    );
    assertEquals("redirect:/home", OrigenDelRegistro.RESUMEN.redireccion(null, null));
  }
}
