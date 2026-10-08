package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.RepositorioGasto;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import com.tallerwebi.dominio.resumen.ServicioResumenImpl;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioResumenImplTest {

  private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);

  private RepositorioGasto repositorioGastoMock;
  private ServicioResumen servicioResumen;
  private Usuario usuario;

  @BeforeEach
  public void init() {
    repositorioGastoMock = mock(RepositorioGasto.class);
    servicioResumen = new ServicioResumenImpl(repositorioGastoMock);
    usuario = new Usuario();
  }

  @Test
  void debeSumar45000GastadosCuandoElMesTieneGastosDe30000Y15000() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(30000.0, "Alquiler"), gasto(15000.0, "Supermercado")));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(45000.0, resumen.getGastado());
    verify(repositorioGastoMock).obtenerGastosDelMes(usuario, SEPTIEMBRE);
  }

  @Test
  void debeTenerDisponible110000Y80PorCientoUsadoCuandoSeGastan440000DeUnPresupuestoDe550000() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(440000.0, "Alquiler")));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(550000.0, resumen.getPresupuesto());
    assertEquals(110000.0, resumen.getDisponible());
    assertEquals(80.0, resumen.getPorcentajeUsado());
  }

  @Test
  void debeTenerDisponibleNegativoCuandoLoGastadoSuperaElPresupuesto() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(600000.0, "Alquiler")));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(-50000.0, resumen.getDisponible());
    assertEquals(109.09, resumen.getPorcentajeUsado(), 0.01);
  }

  @Test
  void debeAgruparEnSinCategoriaLosGastosQueNoTienenCategoriaConEl100PorCientoDelTotal() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(30000.0, "Alquiler"), gasto(15000.0, "Supermercado")));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(1, resumen.getCategorias().size());
    ResumenCategoria categoria = resumen.getCategorias().get(0);
    assertEquals("Sin categoría", categoria.getNombre());
    assertEquals(45000.0, categoria.getGastado());
    assertEquals(100.0, categoria.getPorcentajeTotal());
  }

  @Test
  void debeDevolverTodoEnCeroYSinCategoriasCuandoElMesNoTieneGastos() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE)).thenReturn(List.of());

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(0.0, resumen.getGastado());
    assertEquals(550000.0, resumen.getDisponible());
    assertEquals(0.0, resumen.getPorcentajeUsado());
    assertTrue(resumen.getCategorias().isEmpty());
  }

  private Gasto gasto(Double importe, String descripcion) {
    return new Gasto(importe, LocalDate.of(2026, 9, 10), descripcion);
  }
}
