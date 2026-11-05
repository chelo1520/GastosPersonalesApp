package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.DatosComprobante;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ConfirmacionComprobanteTest {

  @Test
  public void debeArmarUnaPropuestaIncluidaPorCadaGastoDelComprobante() {
    DatosComprobante datos = new DatosComprobante(
      "Compra",
      "ticket de supermercado",
      9800.0,
      List.of(leido(6300.0), leido(3500.0))
    );

    ConfirmacionComprobante confirmacion = ConfirmacionComprobante.de(
      datos,
      List.of(new Categoria())
    );

    assertEquals("Compra", confirmacion.getTipoOperacion());
    assertEquals("ticket de supermercado", confirmacion.getTipoComprobante());
    assertEquals(9800.0, confirmacion.getTotal());
    assertEquals(2, confirmacion.getGastos().size());
    assertEquals(3500.0, confirmacion.getGastos().get(1).getImporte());
    assertTrue(confirmacion.getGastos().get(1).isIncluir());
  }

  private static GastoDelComprobante leido(Double importe) {
    return new GastoDelComprobante(
      importe,
      LocalDate.of(2026, 10, 30),
      "Supermercado Día",
      null,
      null,
      new ConfianzaComprobante(false, false, false, false)
    );
  }
}
