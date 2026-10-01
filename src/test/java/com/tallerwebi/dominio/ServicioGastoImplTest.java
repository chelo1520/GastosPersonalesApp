package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tallerwebi.dominio.excepcion.GastoInvalidoExeption;
import java.time.LocalDate;
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
}
