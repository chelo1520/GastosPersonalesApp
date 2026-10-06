package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import com.tallerwebi.dominio.gasto.GastoServicioImpl;
import com.tallerwebi.dominio.gasto.RepositorioGasto;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

  @BeforeEach
  public void init() {
    this.repositorioGastoMock = mock(RepositorioGasto.class);
    this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
  }

  @Test
  void debeGuardarElGastoAsociadoAlUsuarioCuandoElImporteEsPositivo() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock, HOY);
    Usuario usuario = new Usuario();
    usuario.setEmail("marce@gmail.com");
    usuario.setPassword("123");

    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 10, 13), "Supermercado");

    gastoServicio.registrarGasto(gasto, usuario);

    assertEquals(usuario, gasto.getUsuario());
    verify(repositorioGastoMock).guardar(gasto);
  }

  @Test
  void debeLanzarGastoInvalidoSinGuardarCuandoElImporteEsNegativo() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock, HOY);
    Usuario usuario = new Usuario();
    usuario.setEmail("marce@gmail.com");
    usuario.setPassword("123");

    Gasto gasto = new Gasto(-1000.00, LocalDate.of(2026, 10, 13), "Supermercado");

    assertThrows(GastoInvalidoExeption.class, () -> gastoServicio.registrarGasto(gasto, usuario));
    verify(repositorioGastoMock, never()).guardar(gasto);
  }

  @Test
  void debeLanzarGastoInvalidoSinGuardarCuandoLaFechaEsDelMesAnterior() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock, HOY);
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
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock, HOY);
    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 11, 1), "Supermercado");

    assertThrows(
      GastoInvalidoExeption.class,
      () -> gastoServicio.registrarGasto(gasto, new Usuario())
    );
    verify(repositorioGastoMock, never()).guardar(gasto);
  }

  @Test
  void debeGuardarElGastoCuandoLaFechaEsElPrimerOElUltimoDiaDelMesActual() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock, HOY);
    Gasto primerDia = new Gasto(1000.00, LocalDate.of(2026, 10, 1), "Alquiler");
    Gasto ultimoDia = new Gasto(1000.00, LocalDate.of(2026, 10, 31), "Expensas");

    gastoServicio.registrarGasto(primerDia, new Usuario());
    gastoServicio.registrarGasto(ultimoDia, new Usuario());

    verify(repositorioGastoMock).guardar(primerDia);
    verify(repositorioGastoMock).guardar(ultimoDia);
  }

  @Test
  void debeLanzarGastoInvalidoSinGuardarCuandoLaFechaEsNula() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock, HOY);
    Gasto gasto = new Gasto(1000.00, null, "Supermercado");

    assertThrows(
      GastoInvalidoExeption.class,
      () -> gastoServicio.registrarGasto(gasto, new Usuario())
    );
    verify(repositorioGastoMock, never()).guardar(gasto);
  }

  @Test
  void debePermitirDesdeEl1HastaEl31DeOctubreCuandoHoyEs6DeOctubre() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock, HOY);

    assertEquals(LocalDate.of(2026, 10, 1), gastoServicio.obtenerFechaMinimaPermitida());
    assertEquals(LocalDate.of(2026, 10, 31), gastoServicio.obtenerFechaMaximaPermitida());
  }
}
