package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.asistente.CategoriaGastoReal;
import com.tallerwebi.dominio.asistente.GastoReal;
import com.tallerwebi.dominio.asistente.MesGastoReal;
import com.tallerwebi.dominio.asistente.ServicioAsistente;
import com.tallerwebi.dominio.asistente.ServicioGastoReal;
import com.tallerwebi.dominio.asistente.Veredicto;
import com.tallerwebi.dominio.categoria.Categoria;
import com.tallerwebi.dominio.consejo.Consejo;
import com.tallerwebi.dominio.consejo.EstadoConsejo;
import com.tallerwebi.dominio.consejo.RepositorioConsejo;
import com.tallerwebi.dominio.consejo.ServicioConsejos;
import com.tallerwebi.dominio.consejo.ServicioConsejosImpl;
import com.tallerwebi.dominio.consejo.Severidad;
import com.tallerwebi.dominio.consejo.TipoConsejo;
import com.tallerwebi.dominio.excepcion.ConsejoNoEncontrado;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import com.tallerwebi.dominio.indicadores.ServicioIndicadores;
import com.tallerwebi.dominio.presupuesto.Presupuesto;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import com.tallerwebi.dominio.resumen.ResumenCategoria;
import com.tallerwebi.dominio.resumen.ResumenMensual;
import com.tallerwebi.dominio.resumen.ServicioResumen;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioConsejosImplTest {

  // "Hoy" fijo: 13 de octubre de 2026
  private static final Clock HOY = Clock.fixed(
    Instant.parse("2026-10-13T15:00:00Z"),
    ZoneId.of("America/Argentina/Buenos_Aires")
  );
  private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

  private RepositorioConsejo repositorioMock;
  private ServicioPresupuesto servicioPresupuestoMock;
  private ServicioIndicadores servicioIndicadoresMock;
  private ServicioResumen servicioResumenMock;
  private ServicioGastoReal servicioGastoRealMock;
  private ServicioAsistente servicioAsistenteMock;
  private ServicioConsejos servicio;
  private Usuario usuario;
  private Categoria alimentacion;

  @BeforeEach
  public void init() {
    repositorioMock = mock(RepositorioConsejo.class);
    servicioPresupuestoMock = mock(ServicioPresupuesto.class);
    servicioIndicadoresMock = mock(ServicioIndicadores.class);
    servicioResumenMock = mock(ServicioResumen.class);
    servicioGastoRealMock = mock(ServicioGastoReal.class);
    servicioAsistenteMock = mock(ServicioAsistente.class);
    // Sin IA: el asistente devuelve los consejos tal cual
    when(servicioAsistenteMock.redactar(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    servicio =
      new ServicioConsejosImpl(
        repositorioMock,
        servicioPresupuestoMock,
        servicioIndicadoresMock,
        servicioResumenMock,
        servicioGastoRealMock,
        servicioAsistenteMock,
        HOY
      );
    usuario = new Usuario();
    usuario.setId(1L);
    alimentacion = categoria(1L, "Alimentación");

    // Por defecto: 2 % de inflación todos los meses, sin gastos, sin presupuestos, sin veredicto
    when(servicioIndicadoresMock.inflacionMensual(any()))
      .thenAnswer(inv -> new InflacionMensual(inv.getArgument(0), 2.0, false));
    when(servicioResumenMock.obtenerResumen(any(), any())).thenReturn(resumen(List.of()));
    when(servicioPresupuestoMock.obtenerDelMes(any(), any())).thenReturn(List.of());
    when(servicioGastoRealMock.calcular(any(), anyInt())).thenReturn(gastoReal(null));
    when(repositorioMock.obtenerDelMes(any(), any())).thenReturn(List.of());
  }

  @Test
  void debeSugerirActualizarElPresupuestoCuandoEsElMismoDesdeHaceMesesYLaInflacionLoErosiono() {
    // 100.000 desde julio: la inflación de agosto a octubre (2 % × 3) ya lo pasó del 5 %
    conPresupuestoFijo(alimentacion, 100000.0, YearMonth.of(2026, 7));

    List<Consejo> consejos = servicio.obtenerPendientes(usuario);

    assertThat(consejos, hasSize(1));
    Consejo consejo = consejos.get(0);
    assertThat(consejo.getTipo(), equalTo(TipoConsejo.PRESUPUESTO_DESACTUALIZADO));
    assertThat(consejo.getSeveridad(), equalTo(Severidad.ATENCION));
    assertThat(consejo.getCategoria(), equalTo("Alimentación"));
    // 100.000 × 1,02³ = 106.120,8 → redondeado hacia arriba al millar
    assertThat(consejo.getMontoSugerido(), equalTo(107000.0));
    assertThat(consejo.getTexto(), containsString("desde Julio 2026"));
    assertThat(consejo.getTexto(), containsString("6,1 %"));
  }

  @Test
  void noDebeSugerirNadaCuandoElPresupuestoSeActualizoEsteMes() {
    conPresupuestoFijo(alimentacion, 100000.0, OCTUBRE);

    assertThat(servicio.obtenerPendientes(usuario), hasSize(0));
  }

  @Test
  void noDebeSugerirNadaCuandoLaInflacionAcumuladaTodaviaNoLlegaAlUmbral() {
    // Desde septiembre: solo octubre, 2 %
    conPresupuestoFijo(alimentacion, 100000.0, YearMonth.of(2026, 9));

    assertThat(servicio.obtenerPendientes(usuario), hasSize(0));
  }

  @Test
  void debeAlertarCuandoUnaCategoriaSubeMasQueLaInflacionDosMesesSeguidos() {
    // Entretenimiento: julio 40.000 → agosto 50.000 (+25 %) → septiembre 60.000 (+20 %), con IPC 2 %
    conGastos(YearMonth.of(2026, 7), List.of(cat("Entretenimiento", 40000.0)));
    conGastos(YearMonth.of(2026, 8), List.of(cat("Entretenimiento", 50000.0)));
    conGastos(YearMonth.of(2026, 9), List.of(cat("Entretenimiento", 60000.0)));

    List<Consejo> consejos = servicio.obtenerPendientes(usuario);

    assertThat(consejos, hasSize(1));
    assertThat(consejos.get(0).getTipo(), equalTo(TipoConsejo.CATEGORIA_SUBE_MAS_QUE_LA_INFLACION));
    assertThat(consejos.get(0).getSeveridad(), equalTo(Severidad.ALERTA));
    assertThat(consejos.get(0).getCategoria(), equalTo("Entretenimiento"));
    assertThat(consejos.get(0).getMontoSugerido(), nullValue());
  }

  @Test
  void noDebeAlertarCuandoLaCategoriaSubioUnSoloMes() {
    conGastos(YearMonth.of(2026, 7), List.of(cat("Entretenimiento", 50000.0)));
    conGastos(YearMonth.of(2026, 8), List.of(cat("Entretenimiento", 50000.0)));
    conGastos(YearMonth.of(2026, 9), List.of(cat("Entretenimiento", 60000.0)));

    assertThat(servicio.obtenerPendientes(usuario), hasSize(0));
  }

  @Test
  void debeDarElVeredictoEnRojoConSubidaNominalInflacionYDiferenciaReal() {
    when(servicioGastoRealMock.calcular(any(), anyInt()))
      .thenReturn(
        gastoReal(new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), 30.0, 10.0))
      );

    List<Consejo> consejos = servicio.obtenerPendientes(usuario);

    assertThat(consejos, hasSize(1));
    Consejo veredicto = consejos.get(0);
    assertThat(veredicto.getTipo(), equalTo(TipoConsejo.VEREDICTO_ROJO));
    assertThat(veredicto.isVeredicto(), is(true));
    assertThat(veredicto.isAplicable(), is(false));
    assertThat(veredicto.getTexto(), containsString("Desde Mayo 2026 tus gastos subieron 30,0 %"));
    assertThat(veredicto.getTexto(), containsString("La inflación fue 10,0 %"));
    assertThat(veredicto.getTexto(), containsString("gastás 18,2 % más en serio."));
  }

  @Test
  void debeDarElVeredictoEnVerdeDiciendoQueGastaMenosEnSerioCuandoLaInflacionSuperaLaSubida() {
    when(servicioGastoRealMock.calcular(any(), anyInt()))
      .thenReturn(
        gastoReal(new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), 10.0, 21.0))
      );

    List<Consejo> consejos = servicio.obtenerPendientes(usuario);

    assertThat(consejos, hasSize(1));
    assertThat(consejos.get(0).getTipo(), equalTo(TipoConsejo.VEREDICTO_VERDE));
    assertThat(consejos.get(0).getTexto(), containsString("gastás 9,1 % menos en serio."));
  }

  @Test
  void noDebeDarVeredictoCuandoNoHayDosMesesCerrados() {
    assertThat(servicio.obtenerPendientes(usuario), hasSize(0));
  }

  @Test
  void debeGuardarLosConsejosNuevosYNoRepetirLosQueYaEstabanGuardados() {
    conPresupuestoFijo(alimentacion, 100000.0, YearMonth.of(2026, 7));
    Consejo guardado = new Consejo(
      usuario,
      OCTUBRE,
      TipoConsejo.PRESUPUESTO_DESACTUALIZADO,
      "Alimentación",
      "viejo",
      106000.0
    );
    when(repositorioMock.obtenerDelMes(usuario, OCTUBRE)).thenReturn(List.of(guardado));

    List<Consejo> consejos = servicio.obtenerPendientes(usuario);

    verify(repositorioMock, never()).guardar(any());
    verify(repositorioMock).modificar(guardado);
    assertThat(consejos, hasSize(1));
    assertThat(consejos.get(0).getMontoSugerido(), equalTo(107000.0));
  }

  @Test
  void noDebeDevolverLosConsejosQueElUsuarioDescartoOAplico() {
    conPresupuestoFijo(alimentacion, 100000.0, YearMonth.of(2026, 7));
    Consejo descartado = new Consejo(
      usuario,
      OCTUBRE,
      TipoConsejo.PRESUPUESTO_DESACTUALIZADO,
      "Alimentación",
      "x",
      106000.0
    );
    descartado.setEstado(EstadoConsejo.DESCARTADO);
    when(repositorioMock.obtenerDelMes(usuario, OCTUBRE)).thenReturn(List.of(descartado));

    assertThat(servicio.obtenerPendientes(usuario), hasSize(0));
  }

  @Test
  void debeMostrarElVeredictoLasLecturasYComoMuchoCuatroPropuestasLasMasImportantesPrimero() {
    List<Presupuesto> presupuestos = new ArrayList<>();
    for (int i = 1; i <= 6; i++) {
      presupuestos.add(
        presupuestoFijo(categoria((long) i, "Categoría " + i), 10000.0 * i, YearMonth.of(2026, 7))
      );
    }
    when(servicioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE)).thenReturn(presupuestos);
    when(servicioGastoRealMock.calcular(any(), anyInt()))
      .thenReturn(
        gastoReal(new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), 30.0, 10.0))
      );
    conGastos(YearMonth.of(2026, 7), List.of(cat("Cine", 40000.0)));
    conGastos(YearMonth.of(2026, 8), List.of(cat("Cine", 50000.0)));
    conGastos(YearMonth.of(2026, 9), List.of(cat("Cine", 60000.0)));

    List<Consejo> consejos = servicio.obtenerPendientes(usuario);

    // Veredicto, la lectura de Cine (alerta) y 4 de los 6 presupuestos
    assertThat(consejos, hasSize(6));
    assertThat(consejos.get(0).getTipo(), equalTo(TipoConsejo.VEREDICTO_ROJO));
    assertThat(consejos.get(1).getSeveridad(), equalTo(Severidad.ALERTA));
    assertThat(consejos.get(1).getTipo(), equalTo(TipoConsejo.CATEGORIA_SUBE_MAS_QUE_LA_INFLACION));
    for (int i = 2; i < 6; i++) {
      assertThat(consejos.get(i).getTipo(), equalTo(TipoConsejo.PRESUPUESTO_DESACTUALIZADO));
    }
  }

  @Test
  void debeMostrarComoMuchoTresLecturasAdemasDelVeredictoLasMasImportantesPrimero() {
    // Cinco lecturas posibles: balance, mayor baja, mayor suba, hábitos y hormiga alta
    List<MesGastoReal> meses = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      meses.add(
        new MesGastoReal(
          YearMonth.of(2026, 6 + i),
          760000.0,
          800000.0 - 10000.0 * i,
          2.0,
          false,
          1500.0,
          false,
          60000.0,
          840000.0
        )
      );
    }
    List<CategoriaGastoReal> categorias = List.of(
      new CategoriaGastoReal("Vivienda", 331700.0, 325000.0, 1.7),
      new CategoriaGastoReal("Alimentación", 200000.0, 195000.0, 1.7),
      new CategoriaGastoReal("Transporte", 46800.0, 45700.0, 1.7),
      new CategoriaGastoReal("Servicios", 70200.0, 89000.0, 1.7),
      new CategoriaGastoReal("Entretenimiento", 80000.0, 60000.0, 1.7)
    );
    when(servicioGastoRealMock.calcular(any(), anyInt()))
      .thenReturn(
        new GastoReal(
          meses,
          new Veredicto(YearMonth.of(2026, 6), YearMonth.of(2026, 9), 30.0, 10.0),
          YearMonth.of(2026, 9),
          categorias,
          null,
          true
        )
      );

    List<Consejo> consejos = servicio.obtenerPendientes(usuario);

    assertThat(consejos, hasSize(4));
    assertThat(consejos.get(0).getTipo(), equalTo(TipoConsejo.VEREDICTO_ROJO));
    // Las dos de atención (hormiga y mayor suba) entran seguro; hábitos queda afuera
    assertThat(consejos.get(1).getSeveridad(), equalTo(Severidad.ATENCION));
    assertThat(consejos.get(2).getSeveridad(), equalTo(Severidad.ATENCION));
    assertThat(
      consejos.stream().map(Consejo::getTipo).toList(),
      not(hasItem(TipoConsejo.HABITOS_CON_LA_INFLACION))
    );
  }

  @Test
  void debeActualizarElPresupuestoAlMontoSugeridoYMarcarElConsejoComoAplicadoCuandoSeAplica() {
    conPresupuestoFijo(alimentacion, 100000.0, YearMonth.of(2026, 7));
    Consejo consejo = new Consejo(
      usuario,
      OCTUBRE,
      TipoConsejo.PRESUPUESTO_DESACTUALIZADO,
      "Alimentación",
      "x",
      107000.0
    );
    when(repositorioMock.buscarPorId(5L)).thenReturn(consejo);

    servicio.aplicar(5L, usuario);

    verify(servicioPresupuestoMock).definir(usuario, alimentacion, OCTUBRE, 107000.0);
    assertThat(consejo.getEstado(), equalTo(EstadoConsejo.APLICADO));
    verify(repositorioMock).modificar(consejo);
  }

  @Test
  void debeLanzarConsejoNoEncontradoSinTocarElPresupuestoCuandoElConsejoEsDeOtroUsuario() {
    Usuario otro = new Usuario();
    otro.setId(2L);
    Consejo ajeno = new Consejo(
      otro,
      OCTUBRE,
      TipoConsejo.PRESUPUESTO_DESACTUALIZADO,
      "Alimentación",
      "x",
      107000.0
    );
    when(repositorioMock.buscarPorId(5L)).thenReturn(ajeno);

    assertThrows(ConsejoNoEncontrado.class, () -> servicio.aplicar(5L, usuario));
    verify(servicioPresupuestoMock, never()).definir(any(), any(), any(), any());
  }

  @Test
  void debeMarcarElConsejoComoDescartadoCuandoSeDescarta() {
    Consejo consejo = new Consejo(
      usuario,
      OCTUBRE,
      TipoConsejo.CATEGORIA_SUBE_MAS_QUE_LA_INFLACION,
      "Cine",
      "x",
      null
    );
    when(repositorioMock.buscarPorId(5L)).thenReturn(consejo);

    servicio.descartar(5L, usuario);

    assertThat(consejo.getEstado(), equalTo(EstadoConsejo.DESCARTADO));
    verify(repositorioMock).modificar(consejo);
  }

  @Test
  void debeMostrarLaRedaccionDelAsistenteConElVeredictoSiemprePrimeroYGuardarlaCuandoLaIaResponde() {
    conPresupuestoFijo(alimentacion, 100000.0, YearMonth.of(2026, 7));
    when(servicioGastoRealMock.calcular(any(), anyInt()))
      .thenReturn(
        gastoReal(new Veredicto(YearMonth.of(2026, 5), YearMonth.of(2026, 9), 30.0, 10.0))
      );
    when(servicioAsistenteMock.redactar(any(), any()))
      .thenAnswer(inv -> {
        List<Consejo> recibidos = inv.getArgument(0);
        List<Consejo> alReves = new ArrayList<>(recibidos);
        java.util.Collections.reverse(alReves);
        for (Consejo c : alReves) {
          c.setId(c.getId() == null ? 1L : c.getId());
          c.redactar("Redactado: " + c.getTipo(), c.firmaDeLosHechos());
        }
        return alReves;
      });

    List<Consejo> consejos = servicio.obtenerPendientes(usuario);

    // La IA devolvió [presupuesto, veredicto]; el veredicto igual se muestra arriba
    assertThat(consejos, hasSize(2));
    assertThat(consejos.get(0).getTipo(), equalTo(TipoConsejo.VEREDICTO_ROJO));
    assertThat(consejos.get(0).getTextoParaMostrar(), equalTo("Redactado: VEREDICTO_ROJO"));
    assertThat(consejos.get(1).getTipo(), equalTo(TipoConsejo.PRESUPUESTO_DESACTUALIZADO));
    verify(repositorioMock, times(2)).modificar(any());
  }

  // El mismo monto desde el mes "desde" hasta octubre; antes de "desde", nada
  private void conPresupuestoFijo(Categoria categoria, Double monto, YearMonth desde) {
    Presupuesto actual = presupuestoFijo(categoria, monto, desde);
    when(servicioPresupuestoMock.obtenerDelMes(usuario, OCTUBRE)).thenReturn(List.of(actual));
  }

  private Presupuesto presupuestoFijo(Categoria categoria, Double monto, YearMonth desde) {
    for (YearMonth mes = desde; !mes.isAfter(OCTUBRE); mes = mes.plusMonths(1)) {
      when(servicioPresupuestoMock.buscar(eq(usuario), eq(categoria), eq(mes)))
        .thenReturn(new Presupuesto(usuario, categoria, mes, monto));
    }
    return new Presupuesto(usuario, categoria, OCTUBRE, monto);
  }

  private void conGastos(YearMonth mes, List<ResumenCategoria> categorias) {
    when(servicioResumenMock.obtenerResumen(usuario, mes)).thenReturn(resumen(categorias));
  }

  private ResumenMensual resumen(List<ResumenCategoria> categorias) {
    double total = categorias.stream().mapToDouble(ResumenCategoria::getGastado).sum();
    return new ResumenMensual(total, 0.0, -total, 100.0, categorias);
  }

  private ResumenCategoria cat(String nombre, Double gastado) {
    return new ResumenCategoria(nombre, gastado, 0.0);
  }

  private GastoReal gastoReal(Veredicto veredicto) {
    return new GastoReal(List.of(), veredicto, YearMonth.of(2026, 9), List.of(), null, true);
  }

  private Categoria categoria(Long id, String nombre) {
    Categoria categoria = new Categoria();
    categoria.setId(id);
    categoria.setNombre(nombre);
    return categoria;
  }
}
