package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.categoria.MarcasHormiga;
import com.tallerwebi.dominio.categoria.PreferenciaCategoria;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MarcasHormigaTest {

  private final Usuario usuario = new Usuario();

  @Test
  void sinPreferenciasValeLoQueDiceLaCategoria() {
    MarcasHormiga marcas = MarcasHormiga.porDefecto();

    assertTrue(marcas.esPrescindible(categoria(1L, false, false)));
    assertFalse(marcas.esPrescindible(categoria(2L, true, false)));
  }

  @Test
  void laPreferenciaDelUsuarioPisaALaCategoria() {
    Categoria delivery = categoria(1L, false, false);
    Categoria otros = categoria(2L, true, false);
    MarcasHormiga marcas = MarcasHormiga.de(
      List.of(
        new PreferenciaCategoria(usuario, delivery, true),
        new PreferenciaCategoria(usuario, otros, false)
      )
    );

    assertFalse(marcas.esPrescindible(delivery), "el usuario la marcó esencial");
    assertTrue(marcas.esPrescindible(otros), "el usuario la marcó hormiga");
  }

  @Test
  void unaCategoriaFijaEsEsencialAunqueElUsuarioLaMarque() {
    Categoria salud = categoria(3L, true, true);
    MarcasHormiga marcas = MarcasHormiga.de(
      List.of(new PreferenciaCategoria(usuario, salud, false))
    );

    assertFalse(marcas.esPrescindible(salud));
  }

  @Test
  void unGastoSinCategoriaSeTrataComoEsencial() {
    assertFalse(MarcasHormiga.porDefecto().esPrescindible(null));
  }

  @Test
  void unaPreferenciaSinValorNoCuenta() {
    Categoria delivery = categoria(1L, false, false);
    MarcasHormiga marcas = MarcasHormiga.de(
      List.of(new PreferenciaCategoria(usuario, delivery, null))
    );

    assertTrue(marcas.esPrescindible(delivery));
  }

  private static Categoria categoria(Long id, boolean esencial, boolean fija) {
    Categoria categoria = new Categoria();
    categoria.setId(id);
    categoria.setEsencial(esencial);
    categoria.setFija(fija);
    return categoria;
  }
}
