package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.RepositorioPresupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuestoImpl;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioPresupuestoImplTest {

  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

  private RepositorioPresupuesto repositorioPresupuestoMock;
  private ServicioPresupuesto servicioPresupuesto;
  private Usuario usuario;
  private Categoria alimentacion;

  @BeforeEach
  public void init() {
    repositorioPresupuestoMock = mock(RepositorioPresupuesto.class);
    servicioPresupuesto = new ServicioPresupuestoImpl(repositorioPresupuestoMock);

    usuario = new Usuario();
    usuario.setEmail("diego@unlam.edu.ar");

    alimentacion = new Categoria();
    alimentacion.setId(1L);
    alimentacion.setNombre("Alimentación");
  }

  @Test
  void debeGuardarUnPresupuestoNuevoCuandoLaCategoriaNoTeniaUnoEnEseMes() {
    when(repositorioPresupuestoMock.buscar(usuario, alimentacion, OCTUBRE)).thenReturn(null);

    servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, 130000.0);

    ArgumentCaptor<Presupuesto> guardado = ArgumentCaptor.forClass(Presupuesto.class);
    verify(repositorioPresupuestoMock).guardar(guardado.capture());
    assertThat(guardado.getValue().getUsuario(), equalTo(usuario));
    assertThat(guardado.getValue().getCategoria(), equalTo(alimentacion));
    assertThat(guardado.getValue().getMes(), equalTo(OCTUBRE));
    assertThat(guardado.getValue().getMonto(), equalTo(130000.0));
    verify(repositorioPresupuestoMock, never()).modificar(any());
  }

  @Test
  void debeReemplazarElMontoSinCrearOtroCuandoLaCategoriaYaTeniaPresupuestoEnEseMes() {
    Presupuesto existente = new Presupuesto(usuario, alimentacion, OCTUBRE, 100000.0);
    when(repositorioPresupuestoMock.buscar(usuario, alimentacion, OCTUBRE)).thenReturn(existente);

    servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, 150000.0);

    assertThat(existente.getMonto(), equalTo(150000.0));
    verify(repositorioPresupuestoMock).modificar(existente);
    verify(repositorioPresupuestoMock, never()).guardar(any());
  }

  @Test
  void debeLanzarPresupuestoInvalidoSinGuardarCuandoElMontoEsCero() {
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, 0.0)
    );

    verify(repositorioPresupuestoMock, never()).guardar(any());
    verify(repositorioPresupuestoMock, never()).modificar(any());
  }

  @Test
  void debeLanzarPresupuestoInvalidoSinGuardarCuandoElMontoEsNegativo() {
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, -500.0)
    );

    verify(repositorioPresupuestoMock, never()).guardar(any());
  }

  @Test
  void debeLanzarPresupuestoInvalidoSinGuardarCuandoElMontoEsNulo() {
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, null)
    );

    verify(repositorioPresupuestoMock, never()).guardar(any());
  }

  @Test
  void debeLanzarPresupuestoInvalidoSinGuardarCuandoNoSeEligioCategoria() {
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definir(usuario, null, OCTUBRE, 130000.0)
    );

    verify(repositorioPresupuestoMock, never()).guardar(any());
  }
}
