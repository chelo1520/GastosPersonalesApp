package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.Diagnostico;
import com.tallerwebi.dominio.asistente.EstadoGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.MesGastoReal;
import com.tallerwebi.dominio.asistente.ServicioGastoReal;
import com.tallerwebi.dominio.asistente.ServicioGastoRealImpl;
import com.tallerwebi.dominio.indicadores.CotizacionDolar;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import com.tallerwebi.dominio.indicadores.ServicioIndicadores;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenDeMes;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioGastoRealImplTest {

  // "Hoy" fijo: 13 de octubre de 2026 → el período de 3 meses es agosto, septiembre y octubre
  private static final Clock HOY = Clock.fixed(
    Instant.parse("2026-10-13T15:00:00Z"),
    ZoneId.of("America/Argentina/Buenos_Aires")
  );
  private static final YearMonth AGOSTO = YearMonth.of(2026, 8);
  private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);
  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

  private ServicioResumen servicioResumenMock;
  private ServicioIndicadores servicioIndicadoresMock;
  private ServicioGastoReal servicio;
  private Usuario usuario;

  @BeforeEach
  public void init() {
    servicioResumenMock = mock(ServicioResumen.class);
    servicioIndicadoresMock = mock(ServicioIndicadores.class);
    servicio = new ServicioGastoRealImpl(servicioResumenMock, servicioIndicadoresMock, HOY);
    usuario = new Usuario();

    // Inflación: septiembre 2 % publicada, octubre 2 % estimada; agosto no se usa para deflactar
    when(servicioIndicadoresMock.inflacionMensual(AGOSTO))
      .thenReturn(new InflacionMensual(AGOSTO, 1.7, false));
    when(servicioIndicadoresMock.inflacionMensual(SEPTIEMBRE))
      .thenReturn(new InflacionMensual(SEPTIEMBRE, 2.0, false));
    when(servicioIndicadoresMock.inflacionMensual(OCTUBRE))
      .thenReturn(new InflacionMensual(OCTUBRE, 2.0, true));
    // Dólar: 1.500 al cierre de los meses cerrados, 1.600 hoy
    when(servicioIndicadoresMock.cotizacionDolar(any())).thenReturn(cotizacion(1500.0));
    when(servicioIndicadoresMock.dolarDeHoy()).thenReturn(cotizacion(1600.0));
    when(servicioIndicadoresMock.datosActualizados()).thenReturn(true);

    conHistorial(100000.0, 110000.0, 50000.0);
    conResumen(SEPTIEMBRE, List.of());
    conResumen(AGOSTO, List.of());
  }

  @Test
  void debeExpresarCadaMesEnPesosDeHoyAcumulandoElIpcDeLosMesesSiguientes() {
    GastoReal gastoReal = servicio.calcular(usuario, 3);

    List<MesGastoReal> meses = gastoReal.getMeses();
    assertThat(meses, hasSize(3));
    // Agosto: × 1,02 (septiembre) × 1,02 (octubre)
    assertThat(meses.get(0).getGastadoEnPesosDeHoy(), closeTo(100000.0 * 1.02 * 1.02, 0.01));
    // Septiembre: × 1,02 (octubre)
    assertThat(meses.get(1).getGastadoEnPesosDeHoy(), closeTo(110000.0 * 1.02, 0.01));
    // Octubre es hoy: sin ajuste
    assertThat(meses.get(2).getGastadoEnPesosDeHoy(), equalTo(50000.0));
  }

  @Test
  void debeMarcarElMesEnCursoYPropagarSiElIpcEsEstimado() {
    GastoReal gastoReal = servicio.calcular(usuario, 3);

    MesGastoReal octubre = gastoReal.getMeses().get(2);
    assertThat(octubre.isEnCurso(), is(true));
    assertThat(octubre.isIpcEstimado(), is(true));
    assertThat(gastoReal.getMeses().get(1).isIpcEstimado(), is(false));
  }

  @Test
  void debeConvertirADolaresConElCierreDelMesYConElDolarDeHoyParaElMesEnCurso() {
    GastoReal gastoReal = servicio.calcular(usuario, 3);

    assertThat(gastoReal.getMeses().get(1).getGastadoUsd(), closeTo(110000.0 / 1500.0, 0.01));
    assertThat(gastoReal.getMeses().get(2).getGastadoUsd(), closeTo(50000.0 / 1600.0, 0.01));
  }

  @Test
  void debeCompararElPrimerYElUltimoMesCerradoIgnorandoElMesEnCursoCuandoArmaElVeredicto() {
    GastoReal gastoReal = servicio.calcular(usuario, 3);

    assertThat(gastoReal.getVeredicto().getDesde(), equalTo(AGOSTO));
    assertThat(gastoReal.getVeredicto().getHasta(), equalTo(SEPTIEMBRE));
    assertThat(gastoReal.getUltimoMesCerrado(), equalTo(SEPTIEMBRE));
    // Nominal +10 %, inflación acumulada entre agosto y septiembre 2 % → real ≈ +7,8 %
    assertThat(gastoReal.getVeredicto().getVariacionNominal(), closeTo(10.0, 0.01));
    assertThat(gastoReal.getVeredicto().getInflacionAcumulada(), closeTo(2.0, 0.01));
    assertThat(gastoReal.getVeredicto().getVariacionReal(), closeTo(7.84, 0.01));
    assertThat(gastoReal.getVeredicto().getEstado(), equalTo(EstadoGastoReal.ROJO));
  }

  @Test
  void debeDarVerdeCuandoElGastoSubeMenosQueLaInflacion() {
    conHistorial(100000.0, 101000.0, 50000.0);

    GastoReal gastoReal = servicio.calcular(usuario, 3);

    assertThat(gastoReal.getVeredicto().getVariacionReal(), closeTo(-0.98, 0.01));
    assertThat(gastoReal.getVeredicto().getEstado(), equalTo(EstadoGastoReal.VERDE));
  }

  @Test
  void debeDarAmarilloCuandoElGastoRealSubeHastaUn5PorCiento() {
    conHistorial(100000.0, 105000.0, 50000.0);

    GastoReal gastoReal = servicio.calcular(usuario, 3);

    assertThat(gastoReal.getVeredicto().getVariacionReal(), closeTo(2.94, 0.01));
    assertThat(gastoReal.getVeredicto().getEstado(), equalTo(EstadoGastoReal.AMARILLO));
  }

  @Test
  void debeQuedarSinVeredictoCuandoNoHayDosMesesCerradosConGastos() {
    conHistorial(0.0, 110000.0, 50000.0);

    GastoReal gastoReal = servicio.calcular(usuario, 3);

    assertThat(gastoReal.getVeredicto(), nullValue());
  }

  @Test
  void debeDiagnosticarCadaCategoriaDelUltimoMesCerradoContraElAnteriorYElIpc() {
    conResumen(
      AGOSTO,
      List.of(
        cat("Alimentación", 100000.0),
        cat("Servicios", 50000.0),
        cat("Salud", 20000.0),
        cat("Ropa", 10000.0)
      )
    );
    conResumen(
      SEPTIEMBRE,
      List.of(
        cat("Alimentación", 115000.0),
        cat("Servicios", 51000.0),
        cat("Salud", 15000.0),
        cat("Cine", 8000.0)
      )
    );

    List<CategoriaGastoReal> categorias = servicio.calcular(usuario, 3).getCategorias();

    assertThat(
      categorias.stream().map(CategoriaGastoReal::getNombre).toList(),
      contains("Alimentación", "Servicios", "Salud", "Ropa", "Cine")
    );
    CategoriaGastoReal alimentacion = categorias.get(0);
    assertThat(alimentacion.getVariacionNominal(), closeTo(15.0, 0.01));
    assertThat(alimentacion.getVariacionReal(), closeTo(12.75, 0.01));
    assertThat(alimentacion.getDiagnostico(), equalTo(Diagnostico.CONSUMIS_MAS));
    assertThat(categorias.get(1).getDiagnostico(), equalTo(Diagnostico.SUBIO_EL_PRECIO));
    assertThat(categorias.get(2).getDiagnostico(), equalTo(Diagnostico.AHORRASTE));
    // Ropa: tenía gasto y ahora no → -100 %, ahorraste; Cine: nuevo, sin comparación
    assertThat(categorias.get(3).getVariacionNominal(), closeTo(-100.0, 0.01));
    assertThat(categorias.get(3).getDiagnostico(), equalTo(Diagnostico.AHORRASTE));
    assertThat(categorias.get(4).getVariacionNominal(), nullValue());
    assertThat(categorias.get(4).getDiagnostico(), equalTo(Diagnostico.NUEVO));
  }

  @Test
  void debeInformarQueLosIndicadoresNoEstanAlDiaCuandoElServicioLoDice() {
    when(servicioIndicadoresMock.datosActualizados()).thenReturn(false);

    GastoReal gastoReal = servicio.calcular(usuario, 3);

    assertThat(gastoReal.isIndicadoresAlDia(), is(false));
  }

  private void conHistorial(Double agosto, Double septiembre, Double octubre) {
    when(servicioResumenMock.obtenerHistorial(usuario, OCTUBRE, 3))
      .thenReturn(
        List.of(
          new ResumenDeMes(AGOSTO, agosto, 0.0, 0.0, false),
          new ResumenDeMes(SEPTIEMBRE, septiembre, 0.0, 0.0, false),
          new ResumenDeMes(OCTUBRE, octubre, 0.0, 0.0, true)
        )
      );
  }

  private void conResumen(YearMonth mes, List<ResumenCategoria> categorias) {
    double total = categorias.stream().mapToDouble(ResumenCategoria::getGastado).sum();
    when(servicioResumenMock.obtenerResumen(eq(usuario), eq(mes)))
      .thenReturn(new ResumenMensual(total, 0.0, -total, 100.0, categorias));
  }

  private ResumenCategoria cat(String nombre, Double gastado) {
    return new ResumenCategoria(nombre, gastado, 0.0);
  }

  private CotizacionDolar cotizacion(Double venta) {
    return new CotizacionDolar(LocalDate.of(2026, 9, 30), venta - 50, venta);
  }
}
