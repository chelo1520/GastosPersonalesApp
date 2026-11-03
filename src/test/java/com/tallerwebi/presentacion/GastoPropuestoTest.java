package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.comprobante.ConfianzaComprobante;
import com.tallerwebi.dominio.comprobante.GastoDelComprobante;
import com.tallerwebi.dominio.gasto.Gasto;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GastoPropuestoTest {

  private static final LocalDate FECHA = LocalDate.of(2026, 10, 30);

  private Categoria alimentacion;
  private Categoria snacks;

  @BeforeEach
  public void init() {
    alimentacion = categoria(1L, "Alimentación");
    snacks = categoria(2L, "Café y snacks");
  }

  @Test
  public void debeProponerElGastoLeidoIncluidoYConLaCategoriaQueCoincide() {
    GastoPropuesto propuesto = GastoPropuesto.de(
      leido(" café Y SNACKS ", new ConfianzaComprobante(false, false, false, false)),
      List.of(alimentacion, snacks)
    );

    assertTrue(propuesto.isIncluir());
    assertEquals(3500.0, propuesto.getImporte());
    assertEquals(FECHA, propuesto.getFecha());
    assertEquals("Supermercado Día", propuesto.getDescripcion());
    assertEquals("Caramelos, alfajores", propuesto.getDetalle());
    assertEquals(2L, propuesto.getCategoriaId());
    assertFalse(propuesto.isRevisarCategoria());
  }

  @Test
  public void debePedirRevisarLaCategoriaSiNoCoincideConNingunaExistente() {
    GastoPropuesto propuesto = GastoPropuesto.de(
      leido("Golosinas", new ConfianzaComprobante(false, false, false, false)),
      List.of(alimentacion, snacks)
    );

    assertNull(propuesto.getCategoriaId());
    assertTrue(propuesto.isRevisarCategoria());
  }

  @Test
  public void debeCopiarLasMarcasDeBajaConfianzaDelComprobante() {
    GastoPropuesto propuesto = GastoPropuesto.de(
      leido("Alimentación", new ConfianzaComprobante(true, true, true, true)),
      List.of(alimentacion)
    );

    assertTrue(propuesto.isRevisarImporte());
    assertTrue(propuesto.isRevisarFecha());
    assertTrue(propuesto.isRevisarDescripcion());
    assertTrue(propuesto.isRevisarCategoria());
  }

  @Test
  public void debeConvertirseEnUnGastoConLaCategoriaElegida() {
    GastoPropuesto propuesto = GastoPropuesto.de(
      leido("Alimentación", new ConfianzaComprobante(false, false, false, false)),
      List.of(alimentacion)
    );

    Gasto gasto = propuesto.aGasto(snacks);

    assertEquals(3500.0, gasto.getImporte());
    assertEquals(FECHA, gasto.getFecha());
    assertEquals("Supermercado Día", gasto.getDescripcion());
    assertSame(snacks, gasto.getCategoria());
  }

  private static GastoDelComprobante leido(String categoria, ConfianzaComprobante confianza) {
    return new GastoDelComprobante(
      3500.0,
      FECHA,
      "Supermercado Día",
      "Caramelos, alfajores",
      categoria,
      confianza
    );
  }

  private static Categoria categoria(Long id, String nombre) {
    Categoria categoria = new Categoria();
    categoria.setId(id);
    categoria.setNombre(nombre);
    return categoria;
  }
}
