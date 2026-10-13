package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.RepositorioGasto;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.resumen.EstadoPresupuesto;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenDeMes;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import com.tallerwebi.dominio.resumen.ServicioResumenImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioResumenImplTest {

  private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);
  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

  private RepositorioGasto repositorioGastoMock;
  private ServicioPresupuesto servicioPresupuestoMock;
  private ServicioResumen servicioResumen;
  private Usuario usuario;
  private Categoria vivienda;
  private Categoria alimentacion;

  @BeforeEach
  public void init() {
    repositorioGastoMock = mock(RepositorioGasto.class);
    servicioPresupuestoMock = mock(ServicioPresupuesto.class);
    servicioResumen = new ServicioResumenImpl(repositorioGastoMock, servicioPresupuestoMock);
    usuario = new Usuario();
    vivienda = categoria("Vivienda");
    alimentacion = categoria("Alimentación");
    // Sin presupuestos salvo que el test diga lo contrario
    when(servicioPresupuestoMock.obtenerDelMes(usuario, SEPTIEMBRE)).thenReturn(List.of());
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
  void debeSumarLosPresupuestosDelMesComoPresupuestoTotalCuandoHayVariasCategoriasDefinidas() {
    conPresupuestos(presupuesto(vivienda, 300000.0), presupuesto(alimentacion, 250000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(440000.0, "Alquiler", vivienda)));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(550000.0, resumen.getPresupuesto());
    assertEquals(110000.0, resumen.getDisponible());
    assertEquals(80.0, resumen.getPorcentajeUsado());
  }

  @Test
  void debeTenerDisponibleNegativoCuandoLoGastadoSuperaElPresupuestoDelMes() {
    conPresupuestos(presupuesto(vivienda, 550000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(600000.0, "Alquiler", vivienda)));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(-50000.0, resumen.getDisponible());
    assertEquals(109.09, resumen.getPorcentajeUsado(), 0.01);
  }

  @Test
  void debeMostrarTodoElPresupuestoDisponibleYCeroUsadoCuandoElMesNoTieneGastos() {
    conPresupuestos(presupuesto(vivienda, 550000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE)).thenReturn(List.of());

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(0.0, resumen.getGastado());
    assertEquals(550000.0, resumen.getDisponible());
    assertEquals(0.0, resumen.getPorcentajeUsado());
  }

  @Test
  void debeDevolverPresupuestoCeroYSinCategoriasCuandoElMesNoTieneGastosNiPresupuestos() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE)).thenReturn(List.of());

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(0.0, resumen.getPresupuesto());
    assertEquals(0.0, resumen.getDisponible());
    assertEquals(0.0, resumen.getPorcentajeUsado());
    assertTrue(resumen.getCategorias().isEmpty());
  }

  @Test
  void debeMostrar100PorCientoUsadoCuandoHayGastosPeroNingunPresupuestoDefinido() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(30000.0, "Alquiler", vivienda)));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(0.0, resumen.getPresupuesto());
    assertEquals(-30000.0, resumen.getDisponible());
    assertEquals(100.0, resumen.getPorcentajeUsado());
  }

  @Test
  void debeAgruparEnSinCategoriaConEl100PorCientoDelTotalCuandoLosGastosNoTienenCategoria() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(30000.0, "Alquiler"), gasto(15000.0, "Supermercado")));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(1, resumen.getCategorias().size());
    ResumenCategoria categoria = resumen.getCategorias().get(0);
    assertEquals("Sin categoría", categoria.getNombre());
    assertEquals(45000.0, categoria.getGastado());
    assertEquals(100.0, categoria.getPorcentajeTotal());
    assertEquals(EstadoPresupuesto.SIN_PRESUPUESTO, categoria.getEstado());
  }

  @Test
  void debeAgruparPorCategoriaYOrdenarDeMayorAMenorCuandoHayGastosEnVariasCategorias() {
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(
        List.of(
          gasto(250000.0, "Alquiler", vivienda),
          gasto(40000.0, "Compra semanal", alimentacion),
          gasto(50000.0, "Expensas", vivienda)
        )
      );

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(
      List.of("Vivienda", "Alimentación"),
      resumen.getCategorias().stream().map(ResumenCategoria::getNombre).toList()
    );
    assertEquals(300000.0, resumen.getCategorias().get(0).getGastado());
    assertEquals(40000.0, resumen.getCategorias().get(1).getGastado());
    assertEquals((300000.0 / 340000.0) * 100, resumen.getCategorias().get(0).getPorcentajeTotal());
    assertEquals(340000.0, resumen.getGastado());
  }

  @Test
  void debeTenerDisponible15000YEstadoAmarilloCuandoSeGastan85000DeUnPresupuestoDe100000() {
    conPresupuestos(presupuesto(alimentacion, 100000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(85000.0, "Compra semanal", alimentacion)));

    ResumenCategoria categoria = servicioResumen
      .obtenerResumen(usuario, SEPTIEMBRE)
      .getCategorias()
      .get(0);

    assertEquals(100000.0, categoria.getPresupuesto());
    assertEquals(15000.0, categoria.getDisponible());
    assertEquals(85.0, categoria.getPorcentajeUsado());
    assertEquals(EstadoPresupuesto.AMARILLO, categoria.getEstado());
  }

  @Test
  void debeTenerEstadoVerdeCuandoSeUsaMenosDel80PorCientoDelPresupuestoDeLaCategoria() {
    conPresupuestos(presupuesto(alimentacion, 100000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(79900.0, "Compra semanal", alimentacion)));

    ResumenCategoria categoria = servicioResumen
      .obtenerResumen(usuario, SEPTIEMBRE)
      .getCategorias()
      .get(0);

    assertEquals(EstadoPresupuesto.VERDE, categoria.getEstado());
  }

  @Test
  void debeTenerEstadoAmarilloCuandoSeUsaExactamenteEl80PorCientoDelPresupuestoDeLaCategoria() {
    conPresupuestos(presupuesto(alimentacion, 100000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(80000.0, "Compra semanal", alimentacion)));

    ResumenCategoria categoria = servicioResumen
      .obtenerResumen(usuario, SEPTIEMBRE)
      .getCategorias()
      .get(0);

    assertEquals(EstadoPresupuesto.AMARILLO, categoria.getEstado());
  }

  @Test
  void debeTenerEstadoRojoYDisponibleNegativoCuandoSeSuperaElPresupuestoDeLaCategoria() {
    conPresupuestos(presupuesto(alimentacion, 100000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(120000.0, "Compra semanal", alimentacion)));

    ResumenCategoria categoria = servicioResumen
      .obtenerResumen(usuario, SEPTIEMBRE)
      .getCategorias()
      .get(0);

    assertEquals(-20000.0, categoria.getDisponible());
    assertEquals(120.0, categoria.getPorcentajeUsado());
    assertEquals(EstadoPresupuesto.ROJO, categoria.getEstado());
  }

  @Test
  void debeTenerEstadoRojoCuandoSeUsaExactamenteEl100PorCientoDelPresupuestoDeLaCategoria() {
    conPresupuestos(presupuesto(alimentacion, 100000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(100000.0, "Compra semanal", alimentacion)));

    ResumenCategoria categoria = servicioResumen
      .obtenerResumen(usuario, SEPTIEMBRE)
      .getCategorias()
      .get(0);

    assertEquals(0.0, categoria.getDisponible());
    assertEquals(EstadoPresupuesto.ROJO, categoria.getEstado());
  }

  @Test
  void debeQuedarSinPresupuestoYSinPorcentajeCuandoLaCategoriaTieneGastosPeroNoPresupuesto() {
    conPresupuestos(presupuesto(vivienda, 300000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(40000.0, "Compra semanal", alimentacion)));

    ResumenCategoria categoria = servicioResumen
      .obtenerResumen(usuario, SEPTIEMBRE)
      .getCategorias()
      .stream()
      .filter(c -> c.getNombre().equals("Alimentación"))
      .findFirst()
      .orElseThrow();

    assertNull(categoria.getPresupuesto());
    assertNull(categoria.getDisponible());
    assertNull(categoria.getPorcentajeUsado());
    assertEquals(EstadoPresupuesto.SIN_PRESUPUESTO, categoria.getEstado());
    assertFalse(categoria.isTienePresupuesto());
  }

  @Test
  void debeIncluirLaCategoriaConGastadoCeroCuandoTienePresupuestoPeroNoGastos() {
    conPresupuestos(presupuesto(vivienda, 300000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(gasto(40000.0, "Compra semanal", alimentacion)));

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(
      List.of("Alimentación", "Vivienda"),
      resumen.getCategorias().stream().map(ResumenCategoria::getNombre).toList()
    );
    ResumenCategoria sinGastos = resumen.getCategorias().get(1);
    assertEquals(0.0, sinGastos.getGastado());
    assertEquals(300000.0, sinGastos.getDisponible());
    assertEquals(0.0, sinGastos.getPorcentajeUsado());
    assertEquals(EstadoPresupuesto.VERDE, sinGastos.getEstado());
  }

  private void conPresupuestos(Presupuesto... presupuestos) {
    when(servicioPresupuestoMock.obtenerDelMes(usuario, SEPTIEMBRE))
      .thenReturn(List.of(presupuestos));
  }

  private Presupuesto presupuesto(Categoria categoria, Double monto) {
    return new Presupuesto(usuario, categoria, SEPTIEMBRE, monto);
  }

  @Test
  void debeIncluirLosGastosHormigaYLaAlertaDeAcumuladoCuandoElMesEsElActual() {
    // Hoy: 15 de septiembre. 4 deliveries de 8.000 = 32.000, el 5,8 % del presupuesto
    servicioResumen =
      new ServicioResumenImpl(repositorioGastoMock, servicioPresupuestoMock, reloj("2026-09-15"));
    conPresupuestos(presupuesto(categoria("Vivienda"), 550000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE)).thenReturn(deliveries());

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(32000.0, resumen.getHormiga().getTotal());
    assertEquals(1, resumen.getAlertas().size());
  }

  @Test
  void noDebeDarAlertasCuandoElMesYaCerro() {
    servicioResumen =
      new ServicioResumenImpl(repositorioGastoMock, servicioPresupuestoMock, reloj("2026-10-15"));
    conPresupuestos(presupuesto(categoria("Vivienda"), 550000.0));
    when(repositorioGastoMock.obtenerGastosDelMes(usuario, SEPTIEMBRE)).thenReturn(deliveries());

    ResumenMensual resumen = servicioResumen.obtenerResumen(usuario, SEPTIEMBRE);

    assertEquals(32000.0, resumen.getHormiga().getTotal());
    assertTrue(resumen.getAlertas().isEmpty());
  }

  @Test
  void debeArmarElHistorialDe6MesesConLosMesesSinGastosEnCeroYSoloOctubreEnCurso() {
    servicioResumen =
      new ServicioResumenImpl(repositorioGastoMock, servicioPresupuestoMock, reloj("2026-10-15"));
    // Mismo presupuesto los 6 meses: 550.000, así "chico" es hasta 11.000
    when(servicioPresupuestoMock.totalDelMes(eq(usuario), any(YearMonth.class)))
      .thenReturn(550000.0);
    Gasto alquilerJunio = new Gasto(250000.0, LocalDate.of(2026, 6, 1), "Alquiler");
    List<Gasto> gastos = new ArrayList<>(deliveries());
    gastos.add(alquilerJunio);
    when(
      repositorioGastoMock.obtenerGastosEntreFechas(
        usuario,
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 10, 31)
      )
    )
      .thenReturn(gastos);

    List<ResumenDeMes> historial = servicioResumen.obtenerHistorial(usuario, OCTUBRE, 6);

    assertEquals(
      List.of(
        YearMonth.of(2026, 5),
        YearMonth.of(2026, 6),
        YearMonth.of(2026, 7),
        YearMonth.of(2026, 8),
        YearMonth.of(2026, 9),
        OCTUBRE
      ),
      historial.stream().map(ResumenDeMes::getMes).toList()
    );
    // Junio: solo el alquiler, nada hormiga
    assertEquals(250000.0, historial.get(1).getGastado());
    assertEquals(0.0, historial.get(1).getHormiga());
    // Septiembre: los 4 deliveries de 8.000 son hormiga
    assertEquals(32000.0, historial.get(4).getGastado());
    assertEquals(32000.0, historial.get(4).getHormiga());
    assertEquals(0.0, historial.get(4).getResto());
    // Julio sin gastos
    assertEquals(0.0, historial.get(2).getGastado());
    assertEquals(550000.0, historial.get(2).getPresupuesto());
    assertTrue(historial.get(5).isEnCurso());
    assertFalse(historial.get(4).isEnCurso());
  }

  private Clock reloj(String fecha) {
    return Clock.fixed(Instant.parse(fecha + "T12:00:00Z"), ZoneId.of("UTC"));
  }

  private List<Gasto> deliveries() {
    Categoria delivery = categoria("Delivery");
    delivery.setEsencial(false);
    List<Gasto> gastos = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      Gasto gasto = gasto(8000.0, "Delivery");
      gasto.setCategoria(delivery);
      gastos.add(gasto);
    }
    return gastos;
  }

  private Gasto gasto(Double importe, String descripcion) {
    return new Gasto(importe, LocalDate.of(2026, 9, 10), descripcion);
  }

  private Gasto gasto(Double importe, String descripcion, Categoria categoria) {
    Gasto gasto = gasto(importe, descripcion);
    gasto.setCategoria(categoria);
    return gasto;
  }

  private Categoria categoria(String nombre) {
    Categoria categoria = new Categoria();
    categoria.setNombre(nombre);
    return categoria;
  }
}
