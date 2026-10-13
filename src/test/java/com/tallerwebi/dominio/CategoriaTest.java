package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.categoria.Categoria;
import org.junit.jupiter.api.Test;

public class CategoriaTest {

  @Test
  void debeGuardarIdDescripcionYNombre() {
    Categoria categoria = new Categoria();
    categoria.setId(1L);
    categoria.setDescripcion("Gastos relacionados con alimentos");
    categoria.setNombre("Alimentación");

    assertEquals(1L, categoria.getId());
    assertEquals("Gastos relacionados con alimentos", categoria.getDescripcion());
    assertEquals("Alimentación", categoria.getNombre());
  }

  @Test
  void debeSerEsencialCuandoNoSeMarcoYPrescindibleCuandoSeMarcaComoNoEsencial() {
    Categoria categoria = new Categoria();

    assertTrue(categoria.isEsencial());

    categoria.setEsencial(false);

    assertFalse(categoria.isEsencial());
  }

  @Test
  void debeSerSiempreEsencialCuandoEsFijaAunqueSeLaMarqueComoNoEsencial() {
    Categoria salud = new Categoria();
    salud.setFija(true);
    salud.setEsencial(false);

    assertTrue(salud.isFija());
    assertTrue(salud.isEsencial());
  }
}
