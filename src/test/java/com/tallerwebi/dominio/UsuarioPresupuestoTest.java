package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class UsuarioPresupuestoTest {

  @Test
  void debeCalcularElPresupuestoComoElPorcentajeDelIngreso() {
    Usuario usuario = new Usuario();
    usuario.setIngresoMensual(200000.0);
    usuario.setPorcentajePresupuesto(60.0);

    assertThat(usuario.presupuestoSegunPerfil(), equalTo(120000.0));
  }

  @Test
  void debeUsarElIngresoCompletoCuandoNoSeEligioPorcentaje() {
    Usuario usuario = new Usuario();
    usuario.setIngresoMensual(200000.0);

    assertThat(usuario.presupuestoSegunPerfil(), equalTo(200000.0));
  }

  @Test
  void debeDevolverCeroCuandoNoSeDefinioElIngreso() {
    Usuario usuario = new Usuario();
    usuario.setPorcentajePresupuesto(60.0);

    assertThat(usuario.presupuestoSegunPerfil(), equalTo(0.0));
  }
}
