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
import com.tallerwebi.dominio.presupuesto.PresupuestoTotal;
import com.tallerwebi.dominio.presupuesto.RepositorioPresupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuestoImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
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
    servicioPresupuesto =
      new ServicioPresupuestoImpl(
        repositorioPresupuestoMock,
        Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))
      );

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
  void debeGuardarPorcentajeYCalcularElMontoSegunElTotalVigente() {
    usuario.setIngresoMensual(1000.0);
    servicioPresupuesto.definirPorcentaje(usuario, alimentacion, OCTUBRE, 25.0);
    ArgumentCaptor<Presupuesto> captor = ArgumentCaptor.forClass(Presupuesto.class);
    verify(repositorioPresupuestoMock).guardar(captor.capture());
    Presupuesto guardado = captor.getValue();
    assertThat(guardado.getPorcentaje(), equalTo(25.0));
    when(repositorioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE)).thenReturn(List.of(guardado));
    usuario.setIngresoMensual(2000.0);
    assertThat(servicioPresupuesto.totalDelMes(usuario, OCTUBRE), equalTo(500.0));
    assertThat(guardado.getMonto(), equalTo(250.0));
  }

  @Test
  void debeRechazarUnNuevoTotalQueNoAlcanceParaMontosFijosYPorcentajesRecalculados() {
    Presupuesto variable = new Presupuesto(usuario, alimentacion, OCTUBRE, 500.0);
    variable.setPorcentaje(50.0);
    Categoria transporte = new Categoria();
    Presupuesto fijo = new Presupuesto(usuario, transporte, OCTUBRE, 300.0);
    when(repositorioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE))
      .thenReturn(List.of(variable, fijo));
    when(repositorioPresupuestoMock.obtenerDelUsuario(usuario)).thenReturn(List.of(variable, fijo));

    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definirPresupuestoDelMes(usuario, OCTUBRE, 500.0)
    );
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.validarPresupuestoPorDefecto(usuario, 500.0)
    );
    servicioPresupuesto.definirPresupuestoDelMes(usuario, OCTUBRE, 600.0);
    servicioPresupuesto.validarPresupuestoPorDefecto(usuario, 600.0);
  }

  @Test
  void debeRechazarPorcentajesSinTotalOMayoresAlDisponible() {
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definirPorcentaje(usuario, alimentacion, OCTUBRE, 50.0)
    );
    usuario.setIngresoMensual(1000.0);
    when(repositorioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE))
      .thenReturn(List.of(new Presupuesto(usuario, new Categoria(), OCTUBRE, 600.0)));
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definirPorcentaje(usuario, alimentacion, OCTUBRE, 50.0)
    );
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definirPorcentaje(usuario, alimentacion, OCTUBRE, 101.0)
    );
    verify(repositorioPresupuestoMock, never()).guardar(any());
  }

  @Test
  void debeRedondearElPorcentajeACentavosYUsarElMontoActualAlReemplazarlo() {
    usuario.setIngresoMensual(10.0);
    Presupuesto existente = new Presupuesto(usuario, alimentacion, OCTUBRE, 6.66);
    existente.setPorcentaje(66.67);
    when(repositorioPresupuestoMock.buscar(usuario, alimentacion, OCTUBRE)).thenReturn(existente);
    when(repositorioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE)).thenReturn(List.of(existente));

    servicioPresupuesto.definirPorcentaje(usuario, alimentacion, OCTUBRE, 33.33);
    assertThat(existente.getMonto(), equalTo(3.33));
    assertThat(existente.getPorcentaje(), equalTo(33.33));
    servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, 9.0);
    assertThat(existente.getMonto(), equalTo(9.0));
    assertThat(existente.getPorcentaje(), org.hamcrest.Matchers.nullValue());
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

  @Test
  void debeCalcularLoNoAsignadoComoPresupuestoDelPerfilMenosLaSumaDeCategorias() {
    Categoria transporte = new Categoria();
    transporte.setId(2L);
    transporte.setNombre("Transporte");
    usuario.setIngresoMensual(500000.0);
    usuario.setPorcentajePresupuesto(100.0);
    when(repositorioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE))
      .thenReturn(
        List.of(
          new Presupuesto(usuario, alimentacion, OCTUBRE, 200000.0),
          new Presupuesto(usuario, transporte, OCTUBRE, 100000.0)
        )
      );

    assertThat(servicioPresupuesto.noAsignadoDelMes(usuario, OCTUBRE), equalTo(200000.0));
  }

  @Test
  void debeUsarElPresupuestoDelPerfilComoPresupuestoDelMes() {
    usuario.setIngresoMensual(400000.0);
    usuario.setPorcentajePresupuesto(50.0);

    assertThat(servicioPresupuesto.presupuestoDelMes(usuario, OCTUBRE), equalTo(200000.0));
  }

  @Test
  void debeUsarElTotalDefinidoParaElMesEnVezDelDelPerfil() {
    usuario.setIngresoMensual(400000.0);
    usuario.setPorcentajePresupuesto(50.0);
    when(repositorioPresupuestoMock.buscarTotal(usuario, OCTUBRE))
      .thenReturn(new PresupuestoTotal(usuario, OCTUBRE, 350000.0));

    assertThat(servicioPresupuesto.presupuestoDelMes(usuario, OCTUBRE), equalTo(350000.0));
    assertThat(
      servicioPresupuesto.presupuestoDelMes(usuario, OCTUBRE.plusMonths(1)),
      equalTo(200000.0)
    );
  }

  @Test
  void debeGuardarUnTotalNuevoParaElMes() {
    servicioPresupuesto.definirPresupuestoDelMes(usuario, OCTUBRE, 300000.0);

    ArgumentCaptor<PresupuestoTotal> captor = ArgumentCaptor.forClass(PresupuestoTotal.class);
    verify(repositorioPresupuestoMock).guardarTotal(captor.capture());
    assertThat(captor.getValue().getMonto(), equalTo(300000.0));
    assertThat(captor.getValue().getMes(), equalTo(OCTUBRE));
  }

  @Test
  void debeReemplazarElMontoDelTotalYaDefinidoParaElMes() {
    PresupuestoTotal existente = new PresupuestoTotal(usuario, OCTUBRE, 100000.0);
    when(repositorioPresupuestoMock.buscarTotal(usuario, OCTUBRE)).thenReturn(existente);

    servicioPresupuesto.definirPresupuestoDelMes(usuario, OCTUBRE, 250000.0);

    assertThat(existente.getMonto(), equalTo(250000.0));
    verify(repositorioPresupuestoMock).guardarTotal(existente);
  }

  @Test
  void debeRechazarUnTotalMenorAlYaAsignadoALasCategorias() {
    when(repositorioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE))
      .thenReturn(List.of(new Presupuesto(usuario, alimentacion, OCTUBRE, 150000.0)));

    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definirPresupuestoDelMes(usuario, OCTUBRE, 100000.0)
    );

    verify(repositorioPresupuestoMock, never()).guardarTotal(any());
  }

  @Test
  void debeRechazarUnTotalNuloOCero() {
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definirPresupuestoDelMes(usuario, OCTUBRE, 0.0)
    );
    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definirPresupuestoDelMes(usuario, OCTUBRE, null)
    );
  }

  @Test
  void debeRechazarUnDefectoMenorAlAsignadoEnUnMesQueLoUsa() {
    when(repositorioPresupuestoMock.obtenerDelUsuario(usuario))
      .thenReturn(List.of(new Presupuesto(usuario, alimentacion, OCTUBRE, 594000.0)));

    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.validarPresupuestoPorDefecto(usuario, 450000.0)
    );
  }

  @Test
  void noDebeContarLosMesesYaCerradosAlValidarElDefecto() {
    when(repositorioPresupuestoMock.obtenerDelUsuario(usuario))
      .thenReturn(
        List.of(new Presupuesto(usuario, alimentacion, OCTUBRE.minusMonths(1), 594000.0))
      );

    servicioPresupuesto.validarPresupuestoPorDefecto(usuario, 450000.0);
  }

  @Test
  void noDebeContarLosMesesConTotalPropioAlValidarElDefecto() {
    when(repositorioPresupuestoMock.obtenerDelUsuario(usuario))
      .thenReturn(List.of(new Presupuesto(usuario, alimentacion, OCTUBRE, 594000.0)));
    when(repositorioPresupuestoMock.buscarTotal(usuario, OCTUBRE))
      .thenReturn(new PresupuestoTotal(usuario, OCTUBRE, 700000.0));

    servicioPresupuesto.validarPresupuestoPorDefecto(usuario, 450000.0);
  }

  @Test
  void debeDevolverCeroCuandoElPerfilNoTieneIngreso() {
    assertThat(servicioPresupuesto.presupuestoDelMes(usuario, OCTUBRE), equalTo(0.0));
  }

  @Test
  void debeRechazarUnPresupuestoDeCategoriaQueHaceSuperarElPresupuestoMensual() {
    usuario.setIngresoMensual(200000.0);
    usuario.setPorcentajePresupuesto(100.0);
    Categoria transporte = new Categoria();
    transporte.setId(2L);
    transporte.setNombre("Transporte");
    when(repositorioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE))
      .thenReturn(List.of(new Presupuesto(usuario, alimentacion, OCTUBRE, 150000.0)));

    assertThrows(
      PresupuestoInvalido.class,
      () -> servicioPresupuesto.definir(usuario, transporte, OCTUBRE, 60000.0)
    );

    verify(repositorioPresupuestoMock, never()).guardar(any());
  }

  @Test
  void debePermitirUnPresupuestoDeCategoriaQueLlegaExactoAlPresupuestoMensual() {
    usuario.setIngresoMensual(200000.0);
    usuario.setPorcentajePresupuesto(100.0);

    servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, 200000.0);

    verify(repositorioPresupuestoMock).guardar(any());
  }

  @Test
  void noDebeContarElMontoAnteriorDeLaCategoriaAlModificarlaDentroDelPresupuestoMensual() {
    usuario.setIngresoMensual(200000.0);
    usuario.setPorcentajePresupuesto(100.0);
    Presupuesto existente = new Presupuesto(usuario, alimentacion, OCTUBRE, 150000.0);
    when(repositorioPresupuestoMock.buscar(usuario, alimentacion, OCTUBRE)).thenReturn(existente);
    when(repositorioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE)).thenReturn(List.of(existente));

    servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, 190000.0);

    assertThat(existente.getMonto(), equalTo(190000.0));
  }

  @Test
  void noDebeLimitarLasCategoriasCuandoElPerfilNoDefinioPresupuesto() {
    servicioPresupuesto.definir(usuario, alimentacion, OCTUBRE, 999999.0);

    verify(repositorioPresupuestoMock).guardar(any());
  }
}
