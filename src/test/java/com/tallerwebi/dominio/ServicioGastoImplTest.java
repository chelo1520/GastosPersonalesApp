package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.alerta.Alerta;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.excepcion.GastoNoEncontrado;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.gasto.GastoServicioImpl;
import com.tallerwebi.dominio.gasto.RepositorioGasto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioGastoImplTest {

  // "Hoy" fijo: 6 de octubre de 2026
  private static final Clock HOY = Clock.fixed(
    Instant.parse("2026-10-06T12:00:00Z"),
    ZoneId.of("America/Argentina/Buenos_Aires")
  );

  private RepositorioGasto repositorioGastoMock;
  private RepositorioUsuario repositorioUsuarioMock;
  private ServicioPresupuesto servicioPresupuestoMock;

  @BeforeEach
  public void init() {
    this.repositorioGastoMock = mock(RepositorioGasto.class);
    this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
    this.servicioPresupuestoMock = mock(ServicioPresupuesto.class);
  }

  @Test
  void debeGuardarElGastoAsociadoAlUsuarioCuandoElImporteEsPositivo() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = new Usuario();
    usuario.setEmail("marce@gmail.com");
    usuario.setPassword("123");

    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 10, 13), "Supermercado");

    gastoServicio.registrarGasto(gasto, usuario);

    assertEquals(usuario, gasto.getUsuario());
    verify(repositorioGastoMock).guardar(gasto);
  }

  @Test
  void debeBuscarLosGastosDeLos7DiasHastaLaFechaDelGastoParaLaAlertaDeRepeticion() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = new Usuario();
    Categoria cafe = new Categoria();
    cafe.setId(7L);
    cafe.setNombre("Café y snacks");
    cafe.setEsencial(false);
    List<Gasto> semana = new ArrayList<>();
    for (int dia = 3; dia <= 6; dia++) {
      Gasto cafecito = new Gasto(2500.0, LocalDate.of(2026, 10, dia), "Café");
      cafecito.setCategoria(cafe);
      semana.add(cafecito);
    }
    Gasto nuevo = semana.get(3);
    when(
      repositorioGastoMock.obtenerGastosEntreFechas(
        usuario,
        LocalDate.of(2026, 9, 30),
        LocalDate.of(2026, 10, 6)
      )
    )
      .thenReturn(semana);

    // Presupuesto de octubre: 550.000, así "chico" es hasta 11.000
    when(servicioPresupuestoMock.totalDelMes(usuario, YearMonth.of(2026, 10))).thenReturn(550000.0);

    Alerta alerta = gastoServicio.alertaDeRepeticion(nuevo, usuario);

    assertEquals("Es tu 4.º gasto en Café y snacks esta semana ($10.000)", alerta.getMensaje());
  }

  @Test
  void debeAvisarCuandoElGastoRegistradoHaceLlegarElMesAl80PorCientoDelPresupuesto() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = new Usuario();
    Gasto alquiler = new Gasto(400000.0, LocalDate.of(2026, 10, 1), "Alquiler");
    Gasto supermercado = new Gasto(40000.0, LocalDate.of(2026, 10, 6), "Supermercado");
    when(
      repositorioGastoMock.obtenerGastosEntreFechas(
        usuario,
        LocalDate.of(2026, 10, 1),
        LocalDate.of(2026, 10, 31)
      )
    )
      .thenReturn(List.of(alquiler, supermercado));
    when(servicioPresupuestoMock.totalDelMes(usuario, YearMonth.of(2026, 10))).thenReturn(550000.0);

    Alerta alerta = gastoServicio.alertaDePresupuesto(supermercado, usuario);

    assertEquals(
      "Con este gasto llegaste al 80% del presupuesto del mes ($440.000 de $550.000)",
      alerta.getMensaje()
    );
  }

  @Test
  void debeLanzarGastoInvalidoSinGuardarCuandoElImporteEsNegativo() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = new Usuario();
    usuario.setEmail("marce@gmail.com");
    usuario.setPassword("123");

    Gasto gasto = new Gasto(-1000.00, LocalDate.of(2026, 10, 13), "Supermercado");

    assertThrows(GastoInvalidoExeption.class, () -> gastoServicio.registrarGasto(gasto, usuario));
    verify(repositorioGastoMock, never()).guardar(gasto);
  }

  @Test
  void debeLanzarGastoInvalidoSinGuardarCuandoLaFechaEsDelMesAnterior() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 30), "Supermercado");

    GastoInvalidoExeption error = assertThrows(
      GastoInvalidoExeption.class,
      () -> gastoServicio.registrarGasto(gasto, new Usuario())
    );

    assertEquals(
      "La fecha debe estar dentro del mes actual (entre el 01/10/2026 y el 31/10/2026)",
      error.getMessage()
    );
    verify(repositorioGastoMock, never()).guardar(gasto);
  }

  @Test
  void debeLanzarGastoInvalidoSinGuardarCuandoLaFechaEsDelMesSiguiente() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 11, 1), "Supermercado");

    assertThrows(
      GastoInvalidoExeption.class,
      () -> gastoServicio.registrarGasto(gasto, new Usuario())
    );
    verify(repositorioGastoMock, never()).guardar(gasto);
  }

  @Test
  void debeGuardarElGastoCuandoLaFechaEsElPrimerOElUltimoDiaDelMesActual() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Gasto primerDia = new Gasto(1000.00, LocalDate.of(2026, 10, 1), "Alquiler");
    Gasto ultimoDia = new Gasto(1000.00, LocalDate.of(2026, 10, 31), "Expensas");

    gastoServicio.registrarGasto(primerDia, new Usuario());
    gastoServicio.registrarGasto(ultimoDia, new Usuario());

    verify(repositorioGastoMock).guardar(primerDia);
    verify(repositorioGastoMock).guardar(ultimoDia);
  }

  @Test
  void debeLanzarGastoInvalidoSinGuardarCuandoLaFechaEsNula() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Gasto gasto = new Gasto(1000.00, null, "Supermercado");

    assertThrows(
      GastoInvalidoExeption.class,
      () -> gastoServicio.registrarGasto(gasto, new Usuario())
    );
    verify(repositorioGastoMock, never()).guardar(gasto);
  }

  @Test
  void debeActualizarImporteFechaYDescripcionCuandoSeModificaUnGastoPropioConDatosValidos() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = usuarioConId(1L);
    Gasto gastoGuardado = gastoDe(usuario);
    when(repositorioGastoMock.buscarPorId(10L)).thenReturn(gastoGuardado);

    Gasto datos = new Gasto(2500.00, LocalDate.of(2026, 10, 20), "Farmacia");
    Categoria categoria = new Categoria();
    categoria.setId(2L);
    datos.setCategoria(categoria);
    gastoServicio.modificarGasto(10L, datos, usuario);

    assertEquals(2500.00, gastoGuardado.getImporte());
    assertEquals(LocalDate.of(2026, 10, 20), gastoGuardado.getFecha());
    assertEquals("Farmacia", gastoGuardado.getDescripcion());
    assertEquals(categoria, gastoGuardado.getCategoria());
    assertEquals(usuario, gastoGuardado.getUsuario());
    verify(repositorioGastoMock).modificar(gastoGuardado);
  }

  @Test
  void debeLanzarGastoInvalidoSinModificarCuandoElNuevoImporteEsCero() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = usuarioConId(1L);
    Gasto gastoGuardado = gastoDe(usuario);
    when(repositorioGastoMock.buscarPorId(10L)).thenReturn(gastoGuardado);

    Gasto datos = new Gasto(0.0, LocalDate.of(2026, 10, 20), "Farmacia");

    GastoInvalidoExeption error = assertThrows(
      GastoInvalidoExeption.class,
      () -> gastoServicio.modificarGasto(10L, datos, usuario)
    );

    assertEquals("El importe debe ser mayor a cero", error.getMessage());
    assertEquals(1000.00, gastoGuardado.getImporte());
    verify(repositorioGastoMock, never()).modificar(any());
  }

  @Test
  void debeLanzarGastoInvalidoSinModificarCuandoLaNuevaFechaEstaFueraDelMesActual() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = usuarioConId(1L);
    when(repositorioGastoMock.buscarPorId(10L)).thenReturn(gastoDe(usuario));

    Gasto datos = new Gasto(2500.00, LocalDate.of(2026, 9, 30), "Farmacia");

    assertThrows(
      GastoInvalidoExeption.class,
      () -> gastoServicio.modificarGasto(10L, datos, usuario)
    );
    verify(repositorioGastoMock, never()).modificar(any());
  }

  @Test
  void debeLanzarGastoNoEncontradoSinModificarCuandoElGastoNoExiste() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    when(repositorioGastoMock.buscarPorId(99L)).thenReturn(null);

    Gasto datos = new Gasto(2500.00, LocalDate.of(2026, 10, 20), "Farmacia");

    assertThrows(
      GastoNoEncontrado.class,
      () -> gastoServicio.modificarGasto(99L, datos, usuarioConId(1L))
    );
    verify(repositorioGastoMock, never()).modificar(any());
  }

  @Test
  void debeLanzarGastoNoEncontradoSinModificarCuandoElGastoEsDeOtroUsuario() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    when(repositorioGastoMock.buscarPorId(10L)).thenReturn(gastoDe(usuarioConId(2L)));

    Gasto datos = new Gasto(2500.00, LocalDate.of(2026, 10, 20), "Farmacia");

    assertThrows(
      GastoNoEncontrado.class,
      () -> gastoServicio.modificarGasto(10L, datos, usuarioConId(1L))
    );
    verify(repositorioGastoMock, never()).modificar(any());
  }

  @Test
  void debeEliminarElGastoCuandoPerteneceAlUsuario() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = usuarioConId(1L);
    Gasto gastoGuardado = gastoDe(usuario);
    when(repositorioGastoMock.buscarPorId(10L)).thenReturn(gastoGuardado);

    gastoServicio.eliminarGasto(10L, usuario);

    verify(repositorioGastoMock).eliminar(gastoGuardado);
  }

  @Test
  void debeLanzarGastoNoEncontradoSinEliminarCuandoElGastoNoExiste() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    when(repositorioGastoMock.buscarPorId(99L)).thenReturn(null);

    assertThrows(GastoNoEncontrado.class, () -> gastoServicio.eliminarGasto(99L, usuarioConId(1L)));
    verify(repositorioGastoMock, never()).eliminar(any());
  }

  @Test
  void debeLanzarGastoNoEncontradoSinEliminarCuandoElGastoEsDeOtroUsuario() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    when(repositorioGastoMock.buscarPorId(10L)).thenReturn(gastoDe(usuarioConId(2L)));

    assertThrows(GastoNoEncontrado.class, () -> gastoServicio.eliminarGasto(10L, usuarioConId(1L)));
    verify(repositorioGastoMock, never()).eliminar(any());
  }

  @Test
  void debeDevolverElGastoCuandoPerteneceAlUsuario() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    Usuario usuario = usuarioConId(1L);
    Gasto gastoGuardado = gastoDe(usuario);
    when(repositorioGastoMock.buscarPorId(10L)).thenReturn(gastoGuardado);

    assertEquals(gastoGuardado, gastoServicio.obtenerGasto(10L, usuario));
  }

  @Test
  void debeLanzarGastoNoEncontradoAlObtenerUnGastoDeOtroUsuario() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );
    when(repositorioGastoMock.buscarPorId(10L)).thenReturn(gastoDe(usuarioConId(2L)));

    assertThrows(GastoNoEncontrado.class, () -> gastoServicio.obtenerGasto(10L, usuarioConId(1L)));
  }

  @Test
  void debePermitirDesdeEl1HastaEl31DeOctubreCuandoHoyEs6DeOctubre() {
    GastoServicio gastoServicio = new GastoServicioImpl(
      repositorioGastoMock,
      servicioPresupuestoMock,
      HOY
    );

    assertEquals(LocalDate.of(2026, 10, 1), gastoServicio.obtenerFechaMinimaPermitida());
    assertEquals(LocalDate.of(2026, 10, 31), gastoServicio.obtenerFechaMaximaPermitida());
  }

  private Usuario usuarioConId(Long id) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    return usuario;
  }

  private Gasto gastoDe(Usuario usuario) {
    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 10, 5), "Supermercado");
    gasto.setUsuario(usuario);
    return gasto;
  }
}
