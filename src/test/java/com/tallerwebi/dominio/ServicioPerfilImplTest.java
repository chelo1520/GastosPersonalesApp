package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPerfilImplTest {

  private RepositorioUsuario repositorioUsuarioMock;
  private ServicioPresupuesto servicioPresupuestoMock;
  private ServicioPerfil servicioPerfil;
  private Usuario usuario;

  @BeforeEach
  public void init() {
    repositorioUsuarioMock = mock(RepositorioUsuario.class);
    servicioPresupuestoMock = mock(ServicioPresupuesto.class);
    servicioPerfil = new ServicioPerfilImpl(repositorioUsuarioMock, servicioPresupuestoMock);
    usuario = new Usuario();
    usuario.setEmail("diego@unlam.edu.ar");
  }

  @Test
  void debeGuardarElIngresoYElPorcentajeEnElUsuario() {
    servicioPerfil.definirPresupuesto(usuario, 400000.0, 50.0);

    assertThat(usuario.getIngresoMensual(), equalTo(400000.0));
    assertThat(usuario.getPorcentajePresupuesto(), equalTo(50.0));
    verify(repositorioUsuarioMock).modificar(usuario);
  }

  @Test
  void noDebeGuardarSiElNuevoPresupuestoPorDefectoQuedaPorDebajoDeLoYaAsignado() {
    doThrow(new PresupuestoInvalido("menor a lo asignado"))
      .when(servicioPresupuestoMock)
      .validarPresupuestoPorDefecto(usuario, 450000.0);

    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPerfil.definirPresupuesto(usuario, 900000.0, 50.0)
    );

    verify(repositorioUsuarioMock, never()).modificar(any());
  }

  @Test
  void debeAceptarUnPorcentajeDeCienPorCiento() {
    servicioPerfil.definirPresupuesto(usuario, 400000.0, 100.0);

    assertThat(usuario.getPorcentajePresupuesto(), equalTo(100.0));
  }

  @Test
  void debeRechazarUnIngresoNuloCeroONegativo() {
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPerfil.definirPresupuesto(usuario, null, 50.0)
    );
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPerfil.definirPresupuesto(usuario, 0.0, 50.0)
    );
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPerfil.definirPresupuesto(usuario, -1.0, 50.0)
    );

    verify(repositorioUsuarioMock, never()).modificar(any());
  }

  @Test
  void debeRechazarUnPorcentajeNuloCeroOMayorACien() {
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPerfil.definirPresupuesto(usuario, 400000.0, null)
    );
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPerfil.definirPresupuesto(usuario, 400000.0, 0.0)
    );
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPerfil.definirPresupuesto(usuario, 400000.0, 100.1)
    );

    verify(repositorioUsuarioMock, never()).modificar(any());
  }

  @Test
  void noDebeCambiarElUsuarioCuandoLosDatosSonInvalidos() {
    usuario.setIngresoMensual(300000.0);
    usuario.setPorcentajePresupuesto(80.0);

    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPerfil.definirPresupuesto(usuario, -5.0, 50.0)
    );

    assertThat(usuario.getIngresoMensual(), equalTo(300000.0));
    assertThat(usuario.getPorcentajePresupuesto(), equalTo(80.0));
  }
}
