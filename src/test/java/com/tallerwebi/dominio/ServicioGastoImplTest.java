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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioGastoImplTest {

  private RepositorioGasto repositorioGastoMock;
  private RepositorioUsuario repositorioUsuarioMock;

  @BeforeEach
  public void init() {
    this.repositorioGastoMock = mock(RepositorioGasto.class);
    this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
  }

  @Test
  void deberiaRegistrarUnGastoAsociadoAUnUsuario() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock);
    Usuario usuario = new Usuario();
    usuario.setEmail("marce@gmail.com");
    usuario.setPassword("123");

    Gasto gasto = new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado");

    gastoServicio.registrarGasto(gasto, usuario);

    assertEquals(usuario, gasto.getUsuario());
    verify(repositorioGastoMock).guardar(gasto);
  }

  @Test
  void elImporteNoPuedeSerNegativo() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock);
    Usuario usuario = new Usuario();
    usuario.setEmail("marce@gmail.com");
    usuario.setPassword("123");

    Gasto gasto = new Gasto(-1000.00, LocalDate.of(2026, 9, 13), "Supermercado");

    assertThrows(GastoInvalidoExeption.class, () -> gastoServicio.registrarGasto(gasto, usuario));
    verify(repositorioGastoMock, never()).guardar(gasto);
  }

  @Test
  void deberiaDevolverLosGastosDeSeptiembreSiElMesActualEsOctubre() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock);
    Usuario usuario = new Usuario();
    List<Gasto> gastosDeSeptiembre = List.of(
      new Gasto(1000.00, LocalDate.of(2026, 9, 13), "Supermercado")
    );
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, YearMonth.of(2026, 9)))
      .thenReturn(gastosDeSeptiembre);

    List<Gasto> gastos = gastoServicio.obtenerGastosDelMesAnterior(usuario, YearMonth.of(2026, 10));

    assertEquals(gastosDeSeptiembre, gastos);
  }

  @Test
  void deberiaBuscarLosGastosDeDiciembreDelAnioAnteriorSiElMesActualEsEnero() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock);
    Usuario usuario = new Usuario();

    gastoServicio.obtenerGastosDelMesAnterior(usuario, YearMonth.of(2027, 1));

    verify(repositorioGastoMock).obtenerGastosDelMes(usuario, YearMonth.of(2026, 12));
  }

  @Test
  void deberiaDevolverUnaListaVaciaSiNoHayGastosEnElMesAnterior() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock);
    Usuario usuario = new Usuario();
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, YearMonth.of(2026, 9)))
      .thenReturn(List.of());

    List<Gasto> gastos = gastoServicio.obtenerGastosDelMesAnterior(usuario, YearMonth.of(2026, 10));

    assertTrue(gastos.isEmpty());
  }

  @Test
  void deberiaConsultarSoloElMesAnteriorSinBuscarElMesActualNiTodosLosGastos() {
    GastoServicio gastoServicio = new GastoServicioImpl(repositorioGastoMock);
    Usuario usuario = new Usuario();

    gastoServicio.obtenerGastosDelMesAnterior(usuario, YearMonth.of(2026, 10));

    verify(repositorioGastoMock, never()).obtenerGastosDelMes(usuario, YearMonth.of(2026, 10));
    verify(repositorioGastoMock, never()).BuscarGastosPorUsuario(usuario);
  }
}
