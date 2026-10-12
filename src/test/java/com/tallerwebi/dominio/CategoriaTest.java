package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
